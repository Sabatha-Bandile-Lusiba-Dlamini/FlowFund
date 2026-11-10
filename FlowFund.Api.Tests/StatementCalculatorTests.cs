using FlowFund.Api.Enums;
using FlowFund.Api.Services;
using Xunit;

namespace FlowFund.Api.Tests
{
    public class StatementCalculatorTests
    {
        private static readonly DateTime Today = new(2026, 11, 2);

        [Fact]
        public void Build_SplitsTotalsByStatus_AndGroup()
        {
            var contribs = new[]
            {
                new StatementContribution(1, 500m, Today.AddDays(-40), PaymentStatus.Paid),
                new StatementContribution(1, 500m, Today.AddDays(-10), PaymentStatus.Pending), // overdue
                new StatementContribution(2, 300m, Today.AddDays(10), PaymentStatus.Pending),  // not yet due
                new StatementContribution(2, 300m, Today.AddDays(-5), PaymentStatus.Paid),
            };
            var payouts = new[] { new StatementPayout(2, 3000m) };

            var s = StatementCalculator.Build(contribs, payouts, Today);

            Assert.Equal(800m, s.TotalContributed);
            Assert.Equal(300m, s.TotalPending);
            Assert.Equal(500m, s.TotalOverdue);
            Assert.Equal(3000m, s.TotalPaidOut);
            Assert.Equal(4, s.ContributionCount);
            Assert.Equal(2, s.Groups.Count);
            Assert.Equal(500m, s.Groups.Single(g => g.GroupId == 1).Contributed);
            Assert.Equal(3000m, s.Groups.Single(g => g.GroupId == 2).PaidOut);
        }

        [Fact]
        public void Build_WithNoData_ReturnsZeros_NotSampleFigures()
        {
            var s = StatementCalculator.Build(Array.Empty<StatementContribution>(), Array.Empty<StatementPayout>(), Today);

            Assert.Equal(0m, s.TotalContributed);
            Assert.Equal(0m, s.TotalPaidOut);
            Assert.Empty(s.Groups);
        }

        [Fact]
        public void Build_GroupWithOnlyPayout_StillAppearsInBreakdown()
        {
            var s = StatementCalculator.Build(Array.Empty<StatementContribution>(), new[] { new StatementPayout(7, 100m) }, Today);
            Assert.Equal(7, s.Groups.Single().GroupId);
        }
    }
}
