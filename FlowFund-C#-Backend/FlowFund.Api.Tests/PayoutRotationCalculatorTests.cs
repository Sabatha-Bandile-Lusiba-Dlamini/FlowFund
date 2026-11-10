using FlowFund.Api.Enums;
using FlowFund.Api.Models;
using FlowFund.Api.Services;
using Xunit;

namespace FlowFund.Api.Tests
{
    public class PayoutRotationCalculatorTests
    {
        private static List<Membership> Members(params int[] userIds) =>
            userIds.Select((u, i) => new Membership { Id = i + 1, UserId = u, GroupId = 1, Status = MembershipStatus.Active }).ToList();

        [Fact]
        public void Manual_KeepsGivenOrder()
        {
            var order = new PayoutRotationCalculatorService().OrderMembers(Members(5, 3, 9), RotationStrategy.Manual, new List<int> { 9, 5, 3 });
            Assert.Equal(new[] { 5, 3, 9 }, order);
        }

        [Fact]
        public void JoinDate_UsesJoinOrder_ForActiveMembersOnly()
        {
            var order = new PayoutRotationCalculatorService().OrderMembers(Members(5, 3), RotationStrategy.JoinDate, new List<int> { 9, 3, 5 });
            Assert.Equal(new[] { 3, 5 }, order); // 9 is not active so is skipped
        }

        [Fact]
        public void Random_ReturnsEveryMemberExactlyOnce()
        {
            var order = new PayoutRotationCalculatorService().OrderMembers(Members(1, 2, 3, 4), RotationStrategy.Random, new List<int>());
            Assert.Equal(new[] { 1, 2, 3, 4 }, order.OrderBy(x => x));
        }

        [Fact]
        public void PayoutDate_AddsIntervalPerPosition()
        {
            var start = new DateTime(2026, 8, 18);
            var calc = new PayoutRotationCalculatorService();
            Assert.Equal(start, calc.CalculatePayoutDate(start, 0));
            Assert.Equal(start.AddDays(60), calc.CalculatePayoutDate(start, 2));
        }
    }
}
