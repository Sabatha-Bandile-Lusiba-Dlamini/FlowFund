using FlowFund.Api.Enums;
namespace FlowFund.Api.Models
{
   
    public class Device
    {
        public int Id { get; set;  }
        public int UserId { get; set; }

        public string Token { get; set; } = string.Empty;

        public Platform Platform { get; set; }

        
    }
}
