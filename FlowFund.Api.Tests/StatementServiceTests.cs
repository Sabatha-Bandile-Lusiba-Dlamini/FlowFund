using FlowFund.Api.Enums;
using FlowFund.Api.Models;
using FlowFund.Api.Services;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace FlowFund.Api.Tests
{
    /// <summary>Calls the real StatementService against an in-memory database (no hardcoded sample figures).</summary>
    public class StatementServiceTests
    {
        [Fact]
        public async Task Statement_ComputesFromStoredRows_AndCountsOnlyCompletedPayouts()
        {
            using var db = TestSupport.NewDb(nameof(Statement_ComputesFromStoredRows_AndCountsOnlyCompletedPayouts));
            var me = await TestSupport.AddUserAsync(db);
            db.StokvelGroups.Add(new StokvelGroup { StokvelGroupName = "G", Contribution = 500m });
            await db.SaveChangesAsync();
            var gid = db.StokvelGroups.Single().Id;
            var mine = await TestSupport.AddMemberAsync(db, gid, me.Id, Role.Member);

            db.Contributions.Add(new Contribution { MembershipId = mine.Id, Amount = 500m, DueDate = DateTime.UtcNow.AddDays(-30), Status = PaymentStatus.Paid, ClientGeneratedId = Guid.NewGuid() });
            db.PayoutSchedules.Add(new PayoutSchedule { StokvelGroupId = gid, UserId = me.Id, Amount = 1000m, Status = PayoutStatus.Completed, PayoutDate = DateTime.UtcNow.AddDays(-5), RotationPosition = 1 });
            db.PayoutSchedules.Add(new PayoutSchedule { StokvelGroupId = gid, UserId = me.Id, Amount = 1000m, Status = PayoutStatus.Scheduled, PayoutDate = DateTime.UtcNow.AddDays(25), RotationPosition = 2 });
            await db.SaveChangesAsync();

            var s = await new StatementService(db, NullLogger<StatementService>.Instance)
                .GetMyStatementAsync(me.Id, null, null, null, DateTime.UtcNow);

            Assert.Equal(500m, s.TotalContributed);
            Assert.Equal(1000m, s.TotalPaidOut);   // the Scheduled payout is not counted
            Assert.Equal(1, s.PayoutCount);
        }

        [Fact]
        public async Task Statement_GroupFilter_ExcludesOtherGroups()
        {
            using var db = TestSupport.NewDb(nameof(Statement_GroupFilter_ExcludesOtherGroups));
            var me = await TestSupport.AddUserAsync(db);
            var m1 = await TestSupport.AddMemberAsync(db, 1, me.Id, Role.Member);
            var m2 = await TestSupport.AddMemberAsync(db, 2, me.Id, Role.Member);
            db.Contributions.Add(new Contribution { MembershipId = m1.Id, Amount = 100m, DueDate = DateTime.UtcNow, Status = PaymentStatus.Paid, ClientGeneratedId = Guid.NewGuid() });
            db.Contributions.Add(new Contribution { MembershipId = m2.Id, Amount = 900m, DueDate = DateTime.UtcNow, Status = PaymentStatus.Paid, ClientGeneratedId = Guid.NewGuid() });
            await db.SaveChangesAsync();

            var s = await new StatementService(db, NullLogger<StatementService>.Instance)
                .GetMyStatementAsync(me.Id, groupId: 1, null, null, DateTime.UtcNow);

            Assert.Equal(100m, s.TotalContributed);
        }
    }
}
