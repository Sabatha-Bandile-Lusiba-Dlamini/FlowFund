using FlowFund.Api.Enums;

namespace FlowFund.Api.Services
{
  
    /// The one definition of "overdue". The missed-payments alert, the statement and the unit tests all call it,
    /// so the rule exists once (DRY) and is tested where it lives.
   
    public static class ContributionRules
    {
        //Overdue = already flagged Overdue, or still Pending after its due date has passed.
        public static bool IsOverdue(PaymentStatus status, DateTime dueDate, DateTime asOf) =>
            status == PaymentStatus.Overdue ||
            (status == PaymentStatus.Pending && dueDate.Date < asOf.Date);

        //Whole days past the due date; never negative.
        public static int DaysOverdue(DateTime dueDate, DateTime asOf) =>
            Math.Max(0, (asOf.Date - dueDate.Date).Days);
    }
}
