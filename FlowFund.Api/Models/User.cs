using FlowFund.Api.Enums;
namespace FlowFund.Api.Models
{
    public class User
    {
        public int Id { get; set; }
        public string FullName { get; set; } = string.Empty;

        public string PhoneNumber { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string FirebaseUid { get; set; } = string.Empty;
       public LanguageCode LanguagePreference { get; set; }
        public string Currency { get; set; } = string.Empty;
        public bool BiometricEnabled { get; set; }
        public DateTime CreatedAt { get; set;  }
    }
}
