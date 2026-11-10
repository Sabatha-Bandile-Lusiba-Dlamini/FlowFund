namespace FlowFund.Api.Dtos
{
    public class PayoutScheduleEntryDto
    {
        public string Id { get; set; } = string.Empty;
        public string GroupId { get; set; } = string.Empty;
        public string RecipientUserId { get; set; } = string.Empty;
        public int RotationPosition { get; set; }
        public DateTime PayoutDate { get; set; }
        public decimal Amount { get; set; }
        public string Status { get; set; } = string.Empty;
    }

    public class GenerateScheduleRequest
    {
        public string RotationStrategy { get; set; } = "Random";
        public DateTime CycleStartDate { get; set; }
    }

    public class GenerateScheduleResponse
    {
        public string GroupId { get; set; } = string.Empty;
        public List<PayoutScheduleEntryDto> Entries { get; set; } = new();
    }
}