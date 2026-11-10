using FlowFund.Api.Enums;

namespace FlowFund.Api.Dtos
{
    //PATCH /users/me - null means "leave unchanged"
    public class UpdateProfileDto
    {
        public string? FullName { get; set; }
        public string? PhoneNumber { get; set; }
    }

    //PATCH /users/me/settings - null means "leave unchanged" 
    public class UpdateSettingsDto
    {
        public LanguageCode? LanguagePreference { get; set; }
        public string? Currency { get; set; }
        public bool? BiometricEnabled { get; set; }
    }

    public class NotificationDto
    {
        public int Id { get; set; }
        public string Message { get; set; } = string.Empty;
        public NotificationType Type { get; set; }
        public bool IsRead { get; set; }
    }

    public class GroupStatementDto
    {
        public int GroupId { get; set; }
        public decimal Contributed { get; set; }
        public decimal PaidOut { get; set; }
    }

    //GET /users/me/statements - every figure is computed from stored rows (REQ-STMT-02)
    public class StatementDto
    {
        public decimal TotalContributed { get; set; }
        public decimal TotalPending { get; set; }
        public decimal TotalOverdue { get; set; }
        public decimal TotalPaidOut { get; set; }
        public int ContributionCount { get; set; }
        public int PayoutCount { get; set; }
        public List<GroupStatementDto> Groups { get; set; } = new();
    }
}
