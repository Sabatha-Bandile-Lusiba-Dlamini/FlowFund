namespace FlowFund.Api.Models
{
    public class Meeting
    {
        public int Id { get; set; }
        public int StokvelGroupId { get; set; }
        public string Title { get; set; } = string.Empty;
        public DateTime ScheduledDate { get; set; }
    }
}
