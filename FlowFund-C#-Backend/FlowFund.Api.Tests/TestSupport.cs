using FlowFund.Api.Data;
using FlowFund.Api.Enums;
using FlowFund.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace FlowFund.Api.Tests
{
    //Shared helpers: a fresh in-memory database per test and tiny seed builders.
    internal static class TestSupport
    {
        public static FlowFundDbContext NewDb(string name)
        {
            var options = new DbContextOptionsBuilder<FlowFundDbContext>().UseInMemoryDatabase(name).Options;
            return new FlowFundDbContext(options);
        }

        public static async Task<Membership> AddMemberAsync(FlowFundDbContext db, int groupId, int userId, Role role,
            MembershipStatus status = MembershipStatus.Active)
        {
            var m = new Membership { GroupId = groupId, UserId = userId, Role = role, Status = status };
            db.Memberships.Add(m);
            await db.SaveChangesAsync();
            return m;
        }

        public static async Task<User> AddUserAsync(FlowFundDbContext db, string name = "Test User")
        {
            var u = new User { FullName = name, Email = $"{Guid.NewGuid()}@test.com", FirebaseUid = Guid.NewGuid().ToString(), Currency = "ZAR" };
            db.Users.Add(u);
            await db.SaveChangesAsync();
            return u;
        }
    }
}
