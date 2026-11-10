using FlowFund.Api.Data;
using FlowFund.Api.Dtos;
using FlowFund.Api.Enums;
using FlowFund.Api.Models;
using FlowFund.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace FlowFund.Api.Controllers
{
 
    /// <summary>
    /// Contribution endpoints from the Part 1 contract (sections 4.3 and 4.5): list/log/confirm per group, the
    /// treasurer's missed-payments alert and a member's cross-group history. Access is decided from the
    /// caller's Membership.Role in the database, never from anything in the request body.
    /// </summary>
    [ApiController]
    [Authorize]
    [Route("api/v1")]
    public class ContributionsController : ControllerBase
    {
        private const string Tag = "ContributionsController";
        private readonly FlowFundDbContext _db;
        private readonly CurrentUserService _currentUser;
        private readonly ILogger<ContributionsController> _log;

        public ContributionsController(FlowFundDbContext db, CurrentUserService currentUser, ILogger<ContributionsController> log)
        {
            _db = db;
            _currentUser = currentUser;
            _log = log;
        }

        // GET /api/v1/groups/{groupId}/contributions
        // CHANGED: before, ANY signed-in user could read ANY group's contributions. Now the caller must be an
        // active member; Treasurer/Admin see everyone's rows, a Member sees only their own (REQ-API-03).
        [HttpGet("groups/{groupId:int}/contributions")]
        public async Task<ActionResult<List<ContributionDto>>> GetGroupContributions(int groupId)
        {
            var currentUser = await _currentUser.GetOrCreateCurrentUserAsync();
            var caller = await GetActiveMembershipAsync(groupId, currentUser.Id);
            if (caller == null)
            {
                return ForbiddenResult("not_a_member", "You are not an active member of this group.");
            }

            var seeAll = caller.Role != Role.Member;

            var contributions = await (
                from c in _db.Contributions
                join m in _db.Memberships on c.MembershipId equals m.Id
                where m.GroupId == groupId && (seeAll || m.UserId == currentUser.Id)
                orderby c.DueDate descending, c.Id descending
                select c
            ).ToListAsync();

            return Ok(contributions.Select(ToDto).ToList());
        }

        // POST /api/v1/groups/{groupId}/contributions
        // CHANGED: before, a member could log a contribution against SOMEONE ELSE'S membershipId. Now the
        // membership must belong to the caller (a Treasurer/Admin may log on a member's behalf).
        [HttpPost("groups/{groupId:int}/contributions")]
        public async Task<ActionResult<ContributionDto>> LogContribution(int groupId, [FromBody] CreateContributionRequest request)
        {
            if (request.Amount <= 0)
            {
                return BadRequest(new { error = new { code = "invalid_amount", message = "Amount must be greater than 0." } });
            }

            if (!int.TryParse(request.MembershipId, out var membershipId))
            {
                return BadRequest(new { error = new { code = "invalid_membership", message = "membershipId must be a valid id." } });
            }

            if (request.ClientGeneratedId == default)
            {
                return BadRequest(new { error = new { code = "invalid_client_id", message = "clientGeneratedId is required." } });
            }

            var currentUser = await _currentUser.GetOrCreateCurrentUserAsync();

            var membership = await _db.Memberships.FirstOrDefaultAsync(m => m.Id == membershipId && m.GroupId == groupId);
            if (membership == null)
            {
                return NotFound(new { error = new { code = "membership_not_found", message = "That membership doesn't belong to this group." } });
            }

            if (membership.UserId != currentUser.Id)
            {
                var caller = await GetActiveMembershipAsync(groupId, currentUser.Id);
                if (caller == null || caller.Role == Role.Member)
                {
                    _log.LogWarning("{Tag}: user {UserId} tried to log a contribution for membership {MembershipId} that is not theirs",
                        Tag, currentUser.Id, membershipId);
                    return ForbiddenResult("not_your_membership", "You can only log contributions for your own membership.");
                }
            }

            // Idempotency (REQ-OFF-02/03): a retried sync of the same offline-logged contribution is a no-op.
            var existing = await _db.Contributions.FirstOrDefaultAsync(c => c.ClientGeneratedId == request.ClientGeneratedId);
            if (existing != null)
            {
                if (existing.MembershipId != membershipId)
                {
                    return Conflict(new { error = new { code = "client_id_reused", message = "This clientGeneratedId belongs to a different contribution." } });
                }
                _log.LogInformation("{Tag}: replayed client id, returning stored contribution {Id}", Tag, existing.Id);
                return Ok(ToDto(existing));
            }

            var contribution = new Contribution
            {
                MembershipId = membershipId,
                Amount = request.Amount,
                DueDate = DateTime.UtcNow.Date, // TODO (You): derive from the group's contribution schedule rather than "today"
                Status = PaymentStatus.Pending,
                PaymentReference = request.PaymentReference,
                ClientGeneratedId = request.ClientGeneratedId,
                CreatedAt = DateTime.UtcNow,
            };

            _db.Contributions.Add(contribution);
            try
            {
                await _db.SaveChangesAsync();
            }
            catch (DbUpdateException ex)
            {
                // Two identical retries arrived together; if a unique index on ClientGeneratedId exists the second
                // insert fails here. Return the winner instead of a 500.
                var winner = await _db.Contributions.AsNoTracking().FirstOrDefaultAsync(c => c.ClientGeneratedId == request.ClientGeneratedId);
                if (winner == null) throw;
                _log.LogWarning(ex, "{Tag}: concurrent duplicate resolved to contribution {Id}", Tag, winner.Id);
                return Ok(ToDto(winner));
            }

            _log.LogInformation("{Tag}: user {UserId} logged contribution {Id} of {Amount} in group {GroupId}",
                Tag, currentUser.Id, contribution.Id, contribution.Amount, groupId);
            return CreatedAtAction(nameof(GetGroupContributions), new { groupId }, ToDto(contribution));
        }

        // PATCH /api/v1/groups/{groupId}/contributions/{contributionId}
        // CHANGED: caller must be ACTIVE (a Removed Treasurer could confirm before); confirming twice is harmless;
        // the contributor now gets an in-app notification (REQ-NOTIF-04); status + notification save together.
        [HttpPatch("groups/{groupId:int}/contributions/{contributionId:int}")]
        public async Task<ActionResult<ContributionDto>> ConfirmContribution(int groupId, int contributionId, [FromBody] ConfirmContributionRequest? request = null)
        {
            var currentUser = await _currentUser.GetOrCreateCurrentUserAsync();

            var callerMembership = await GetActiveMembershipAsync(groupId, currentUser.Id);
            if (callerMembership == null || callerMembership.Role == Role.Member)
            {
                _log.LogWarning("{Tag}: user {UserId} was refused permission to confirm contribution {Id}", Tag, currentUser.Id, contributionId);
                return ForbiddenResult("not_treasurer", "Only a Treasurer or Admin can confirm payments."); // REQ-MEM-01
            }

            var row = await (
                from c in _db.Contributions
                join m in _db.Memberships on c.MembershipId equals m.Id
                where c.Id == contributionId && m.GroupId == groupId
                select new { Contribution = c, ContributorUserId = m.UserId }
            ).FirstOrDefaultAsync();

            if (row == null)
            {
                return NotFound(new { error = new { code = "contribution_not_found", message = "Contribution not found in this group." } });
            }

            var contribution = row.Contribution;
            if (contribution.Status == PaymentStatus.Paid)
            {
                return Ok(ToDto(contribution)); // already confirmed: no second notification
            }

            contribution.Status = PaymentStatus.Paid;
            contribution.PaidDate = DateTime.UtcNow;
            contribution.ConfirmedByUserId = currentUser.Id;

            _db.Notifications.Add(new Notification
            {
                UserId = row.ContributorUserId,
                NotificationType = NotificationType.Confirmation,
                IsRead = false,
                Message = string.Format(System.Globalization.CultureInfo.InvariantCulture,
                    "Your contribution of R{0:0.00} was confirmed.", contribution.Amount)
            });

            await _db.SaveChangesAsync();
            _log.LogInformation("{Tag}: contribution {Id} confirmed by user {UserId}", Tag, contributionId, currentUser.Id);
            return Ok(ToDto(contribution));
        }

        // GET /api/v1/groups/{groupId}/alerts/missed-payments   (NEW - REQ-ALERT-01, Treasurer/Admin only)
        // "Missed" uses ContributionRules.IsOverdue (plain C#, so filtered after loading; fine at 5-30 members, REQ-NFR-02).
        [HttpGet("groups/{groupId:int}/alerts/missed-payments")]
        public async Task<ActionResult<List<ContributionDto>>> GetMissedPayments(int groupId)
        {
            var currentUser = await _currentUser.GetOrCreateCurrentUserAsync();
            var caller = await GetActiveMembershipAsync(groupId, currentUser.Id);
            if (caller == null || caller.Role == Role.Member)
            {
                return ForbiddenResult("not_treasurer", "Only a Treasurer or Admin can view missed payments.");
            }

            var open = await (
                from c in _db.Contributions
                join m in _db.Memberships on c.MembershipId equals m.Id
                where m.GroupId == groupId && c.Status != PaymentStatus.Paid
                select c
            ).ToListAsync();

            var today = DateTime.UtcNow;
            return Ok(open
                .Where(c => ContributionRules.IsOverdue(c.Status, c.DueDate, today))
                .OrderBy(c => c.DueDate)
                .Select(ToDto)
                .ToList());
        }

        // GET /api/v1/users/me/contributions[?groupId=&from=&to=]
        // CHANGED: optional filters by group and due-date range (REQ-CONTRIB-02 "filterable by date and group").
        [HttpGet("users/me/contributions")]
        public async Task<ActionResult<List<ContributionDto>>> GetMyContributions(
            [FromQuery] int? groupId,
            [FromQuery(Name = "from")] DateTime? fromDate,
            [FromQuery(Name = "to")] DateTime? toDate)
        {
            var currentUser = await _currentUser.GetOrCreateCurrentUserAsync();

            var contributions = await (
                from c in _db.Contributions
                join m in _db.Memberships on c.MembershipId equals m.Id
                where m.UserId == currentUser.Id
                      && (groupId == null || m.GroupId == groupId)
                      && (fromDate == null || c.DueDate >= fromDate)
                      && (toDate == null || c.DueDate <= toDate)
                orderby c.DueDate descending, c.Id descending
                select c
            ).ToListAsync();

            return Ok(contributions.Select(ToDto).ToList());
        }

        // helpers 

        private Task<Membership?> GetActiveMembershipAsync(int groupId, int userId) =>
            _db.Memberships.FirstOrDefaultAsync(m =>
                m.GroupId == groupId && m.UserId == userId && m.Status == MembershipStatus.Active);

        // Same error envelope as the rest of this controller. Replaces a bare Forbid(), which depends on the
        // authentication scheme's forbid handler instead of simply answering 403.
        private ObjectResult ForbiddenResult(string code, string message) =>
            StatusCode(StatusCodes.Status403Forbidden, new { error = new { code, message } });

        private static ContributionDto ToDto(Contribution c) => new()
        {
            Id = c.Id.ToString(),
            MembershipId = c.MembershipId.ToString(),
            Amount = c.Amount,
            DueDate = c.DueDate,
            PaidDate = c.PaidDate,
            Status = c.Status.ToString(),
            PaymentReference = c.PaymentReference,
        };
    }
}