namespace FlowFund.Api.Dtos
{
    
    public class ContributionDto
    {
        public string Id { get; set; } = string.Empty;
        public string MembershipId { get; set; } = string.Empty;
        public decimal Amount { get; set; }
        public DateTime DueDate { get; set; }
        public DateTime? PaidDate { get; set; }
        public string Status { get; set; } = string.Empty;
        public string? PaymentReference { get; set; }
    }

    public class CreateContributionRequest
    {
        public string MembershipId { get; set; } = string.Empty;
        public decimal Amount { get; set; }
        public string? PaymentReference { get; set; }
        public Guid ClientGeneratedId { get; set; }
    }

    public class ConfirmContributionRequest
    {
        public string? Status { get; set; }
    }
}