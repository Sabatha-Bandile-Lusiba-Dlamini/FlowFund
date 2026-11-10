using FlowFund.Api.Enums;
using FlowFund.Api.Models;

namespace FlowFund.Api.Services
{
    public class PayoutRotationCalculatorService
    {
        public List<int> OrderMembers(
            List<Membership> activeMembers,
            RotationStrategy strategy,
            List<int> joinOrderUserIds)
        {
            var userIds = activeMembers
                .Select(m => m.UserId)
                .ToList();

            return strategy switch
            {
                RotationStrategy.Random =>
                    userIds.OrderBy(_ => Guid.NewGuid()).ToList(),

                RotationStrategy.JoinDate =>
                    joinOrderUserIds
                        .Where(userIds.Contains)
                        .ToList(),

                RotationStrategy.Manual =>
                    userIds,

                _ => userIds
            };
        }

        public DateTime CalculatePayoutDate(
            DateTime cycleStartDate,
            int rotationPosition,
            int intervalDays = 30)
        {
            return cycleStartDate.AddDays(intervalDays * rotationPosition);
        }
    }
}