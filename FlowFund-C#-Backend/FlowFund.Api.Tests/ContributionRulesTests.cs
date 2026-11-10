using FlowFund.Api.Enums;
using FlowFund.Api.Services;
using Xunit;

namespace FlowFund.Api.Tests
{
    public class ContributionRulesTests
    {
        private static readonly DateTime Today = new(2026, 11, 2);

        [Fact]
        public void PendingAfterDueDate_IsOverdue() =>
            Assert.True(ContributionRules.IsOverdue(PaymentStatus.Pending, Today.AddDays(-1), Today));

        [Fact]
        public void PendingOnDueDate_IsNotYetOverdue() =>
            Assert.False(ContributionRules.IsOverdue(PaymentStatus.Pending, Today, Today));

        [Fact]
        public void Paid_IsNeverOverdue() =>
            Assert.False(ContributionRules.IsOverdue(PaymentStatus.Paid, Today.AddDays(-30), Today));

        [Fact]
        public void ExplicitOverdueStatus_IsOverdue() =>
            Assert.True(ContributionRules.IsOverdue(PaymentStatus.Overdue, Today.AddDays(5), Today));

        [Fact]
        public void DaysOverdue_CountsWholeDays_AndNeverGoesNegative()
        {
            Assert.Equal(12, ContributionRules.DaysOverdue(Today.AddDays(-12), Today));
            Assert.Equal(0, ContributionRules.DaysOverdue(Today.AddDays(3), Today));
        }
    }
}
