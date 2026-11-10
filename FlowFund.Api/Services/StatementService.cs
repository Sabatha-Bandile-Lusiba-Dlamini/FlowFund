using FlowFund.Api.Data;
using FlowFund.Api.Dtos;
using FlowFund.Api.Enums;
using Microsoft.EntityFrameworkCore;

namespace FlowFund.Api.Services
{
    //Loads the caller's rows and hands them to StatementCalculator (REQ-STMT-02).
    public class StatementService : IStatementService
    {
        private const string Tag = "StatementService";
        private readonly FlowFundDbContext _db;
        private readonly ILogger<StatementService> _log;

        public StatementService(FlowFundDbContext db, ILogger<StatementService> log)
        {
            _db = db;
            _log = log;
        }

        public async Task<StatementDto> GetMyStatementAsync(int userId, int? groupId, DateTime? fromDate, DateTime? toDate, DateTime asOf)
        {
            var contributions = await (from c in _db.Contributions
                                       join m in _db.Memberships on c.MembershipId equals m.Id
                                       where m.UserId == userId
                                             && (groupId == null || m.GroupId == groupId)
                                             && (fromDate == null || c.DueDate >= fromDate)
                                             && (toDate == null || c.DueDate <= toDate)
                                       select new StatementContribution(m.GroupId, c.Amount, c.DueDate, c.Status)).ToListAsync();

            // PayoutSchedule now stores the pot amount, so no derivation is needed. Only Completed payouts count.
            var payoutRows = await _db.PayoutSchedules
                .Where(p => p.UserId == userId
                            && p.Status == PayoutStatus.Completed
                            && (groupId == null || p.StokvelGroupId == groupId)
                            && (fromDate == null || p.PayoutDate >= fromDate)
                            && (toDate == null || p.PayoutDate <= toDate))
                .Select(p => new { p.StokvelGroupId, p.Amount })
                .ToListAsync();

            var payouts = payoutRows.Select(p => new StatementPayout(p.StokvelGroupId, p.Amount)).ToList();

            _log.LogInformation("{Tag}: built statement for user {UserId}: {C} contributions, {P} payouts",
                Tag, userId, contributions.Count, payouts.Count);
            return StatementCalculator.Build(contributions, payouts, asOf);
        }
    }
}
