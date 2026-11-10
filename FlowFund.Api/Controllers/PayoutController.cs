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
    //Payout rotation endpoints (REQ-PAYOUT-01/03). Members read; Treasurer/Admin generate.
    [ApiController]
    [Authorize]
    [Route("api/v1/groups/{groupId:int}/payout-schedule")]
    public class PayoutController : ControllerBase
    {
        private const string Tag = "PayoutController";
        private readonly FlowFundDbContext _db;
        private readonly CurrentUserService _currentUser;
        private readonly PayoutRotationCalculatorService _calculator;
        private readonly ILogger<PayoutController> _log;

        public PayoutController(FlowFundDbContext db, CurrentUserService currentUser,
            PayoutRotationCalculatorService calculator, ILogger<PayoutController> log)
        {
            _db = db;
            _currentUser = currentUser;
            _calculator = calculator;
            _log = log;
        }

        [HttpGet]
        public async Task<ActionResult<List<PayoutScheduleEntryDto>>> GetSchedule(int groupId)
        {
            var currentUser = await _currentUser.GetOrCreateCurrentUserAsync();
            if (await GetActiveMembershipAsync(groupId, currentUser.Id) == null)
            {
                return ForbiddenResult("not_a_member", "You are not an active member of this group.");
            }

            var entries = await _db.PayoutSchedules
                .Where(p => p.StokvelGroupId == groupId)
                .OrderBy(p => p.RotationPosition)
                .ToListAsync();

            return Ok(entries.Select(ToDto).ToList());
        }

        [HttpGet("next")]
        public async Task<ActionResult<PayoutScheduleEntryDto>> GetNext(int groupId)
        {
            var currentUser = await _currentUser.GetOrCreateCurrentUserAsync();
            if (await GetActiveMembershipAsync(groupId, currentUser.Id) == null)
            {
                return ForbiddenResult("not_a_member", "You are not an active member of this group.");
            }

            var next = await _db.PayoutSchedules
                .Where(p => p.StokvelGroupId == groupId && p.Status == PayoutStatus.Scheduled)
                .OrderBy(p => p.RotationPosition)
                .FirstOrDefaultAsync();

            if (next == null)
            {
                return NotFound(new { error = new { code = "no_next_payout", message = "No scheduled payout for this group." } });
            }
            return Ok(ToDto(next));
        }

        [HttpPost("generate")]
        public async Task<ActionResult<GenerateScheduleResponse>> Generate(int groupId, [FromBody] GenerateScheduleRequest request)
        {
            var currentUser = await _currentUser.GetOrCreateCurrentUserAsync();
            var callerMembership = await GetActiveMembershipAsync(groupId, currentUser.Id);
            if (callerMembership == null || callerMembership.Role == Role.Member)
            {
                _log.LogWarning("{Tag}: user {UserId} refused permission to generate a schedule for group {GroupId}", Tag, currentUser.Id, groupId);
                return ForbiddenResult("not_treasurer", "Only a Treasurer or Admin can generate the schedule."); // REQ-PAYOUT-01
            }

            var group = await _db.StokvelGroups.FirstOrDefaultAsync(g => g.Id == groupId);
            if (group == null)
            {
                return NotFound(new { error = new { code = "group_not_found", message = "Group not found." } });
            }

            if (!Enum.TryParse<RotationStrategy>(request.RotationStrategy, true, out var strategy))
            {
                return BadRequest(new { error = new { code = "invalid_strategy", message = "rotationStrategy must be Random, JoinDate, or Manual." } });
            }

            var activeMembers = await _db.Memberships
                .Where(m => m.GroupId == groupId && m.Status == MembershipStatus.Active)
                .ToListAsync();

            if (activeMembers.Count == 0)
            {
                return BadRequest(new { error = new { code = "no_members", message = "Group has no active members to schedule." } });
            }

            // Clear any existing scheduled (not yet completed) entries before regenerating.
            var existingScheduled = _db.PayoutSchedules.Where(p => p.StokvelGroupId == groupId && p.Status == PayoutStatus.Scheduled);
            _db.PayoutSchedules.RemoveRange(existingScheduled);

            // Memberships are loaded in Id order, which is the order people joined (so JoinDate works).
            var joinOrder = activeMembers.OrderBy(m => m.Id).Select(m => m.UserId).ToList();
            var orderedUserIds = _calculator.OrderMembers(activeMembers, strategy, joinOrder);

            var newEntries = new List<PayoutSchedule>();
            for (int i = 0; i < orderedUserIds.Count; i++)
            {
                newEntries.Add(new PayoutSchedule
                {
                    StokvelGroupId = groupId,
                    UserId = orderedUserIds[i],
                    RotationPosition = i + 1,
                    PayoutDate = _calculator.CalculatePayoutDate(request.CycleStartDate, i),
                    Amount = group.Contribution * activeMembers.Count,
                    Status = PayoutStatus.Scheduled,
                    CreatedAt = DateTime.UtcNow,
                });
            }

            _db.PayoutSchedules.AddRange(newEntries);
            await _db.SaveChangesAsync();
            _log.LogInformation("{Tag}: user {UserId} generated a {Strategy} schedule of {Count} entries for group {GroupId}",
                Tag, currentUser.Id, strategy, newEntries.Count, groupId);

            return Ok(new GenerateScheduleResponse
            {
                GroupId = groupId.ToString(),
                Entries = newEntries.Select(ToDto).ToList(),
            });
        }

        private Task<Membership?> GetActiveMembershipAsync(int groupId, int userId) =>
            _db.Memberships.FirstOrDefaultAsync(m =>
                m.GroupId == groupId && m.UserId == userId && m.Status == MembershipStatus.Active);

        private ObjectResult ForbiddenResult(string code, string message) =>
            StatusCode(StatusCodes.Status403Forbidden, new { error = new { code, message } });

        private static PayoutScheduleEntryDto ToDto(PayoutSchedule p) => new()
        {
            Id = p.Id.ToString(),
            GroupId = p.StokvelGroupId.ToString(),
            RecipientUserId = p.UserId.ToString(),
            RotationPosition = p.RotationPosition,
            PayoutDate = p.PayoutDate,
            Amount = p.Amount,
            Status = p.Status.ToString(),
        };
    }
}
