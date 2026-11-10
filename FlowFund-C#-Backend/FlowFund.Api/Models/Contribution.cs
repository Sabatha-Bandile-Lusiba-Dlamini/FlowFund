using Microsoft.EntityFrameworkCore;
using FlowFund.Api.Enums;

namespace FlowFund.Api.Models
{
    public class Contribution
    {
        public int Id { get; set; }
        public int MembershipId { get; set; }
        public decimal Amount { get; set; }
        public DateTime DueDate { get; set; }
        public PaymentStatus Status { get; set; }

        // Idempotency key generated once on the phone, so a retried offline sync cannot create a duplicate.
        public Guid ClientGeneratedId { get; set; }

        // "Contribution Detail" screen: when it was confirmed, the member's payment reference
        // (e.g. "EFT-24"), who confirmed it, and when the row was created.
        public DateTime? PaidDate { get; set; }
        public string? PaymentReference { get; set; }
        public int? ConfirmedByUserId { get; set; }
        public DateTime CreatedAt { get; set; }
    }
}
