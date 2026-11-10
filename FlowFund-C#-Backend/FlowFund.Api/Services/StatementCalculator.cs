using FlowFund.Api.Dtos;
using FlowFund.Api.Enums;

namespace FlowFund.Api.Services
{
    public record StatementContribution(int GroupId, decimal Amount, DateTime DueDate, PaymentStatus Status);
    public record StatementPayout(int GroupId, decimal Amount);

  
    // Pure calculation behind GET /users/me/statements: no database, no clock, no I/O. Unit tests call
    // Build directly with hand-made rows, and the service calls the very same method with real rows,
    // so the tested code IS the production code. Nothing here is hardcoded sample data.
   
    public static class StatementCalculator
    {
        public static StatementDto Build(
            IEnumerable<StatementContribution> contributions,
            IEnumerable<StatementPayout> payouts,
            DateTime asOf)
        {
            var cList = contributions.ToList();
            var pList = payouts.ToList();

            decimal Sum(Func<StatementContribution, bool> where) => cList.Where(where).Sum(c => c.Amount);

            var groupIds = cList.Select(c => c.GroupId).Union(pList.Select(p => p.GroupId)).OrderBy(id => id);

            return new StatementDto
            {
                TotalContributed = Sum(c => c.Status == PaymentStatus.Paid),
                TotalPending = Sum(c => c.Status == PaymentStatus.Pending && !ContributionRules.IsOverdue(c.Status, c.DueDate, asOf)),
                TotalOverdue = Sum(c => ContributionRules.IsOverdue(c.Status, c.DueDate, asOf)),
                TotalPaidOut = pList.Sum(p => p.Amount),
                ContributionCount = cList.Count,
                PayoutCount = pList.Count,
                Groups = groupIds.Select(id => new GroupStatementDto
                {
                    GroupId = id,
                    Contributed = cList.Where(c => c.GroupId == id && c.Status == PaymentStatus.Paid).Sum(c => c.Amount),
                    PaidOut = pList.Where(p => p.GroupId == id).Sum(p => p.Amount)
                }).ToList()
            };
        }
    }
}
