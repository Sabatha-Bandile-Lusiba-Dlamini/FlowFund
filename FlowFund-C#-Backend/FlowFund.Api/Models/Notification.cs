using FlowFund.Api.Enums;
namespace FlowFund.Api.Models
{
   
    public class Notification
    {
        public int Id { get; set; }
        public int UserId { get; set;  }

        public string Message { get; set; } = string.Empty;
        public NotificationType NotificationType { get; set; }

        public bool IsRead { get; set; }
    }
}
