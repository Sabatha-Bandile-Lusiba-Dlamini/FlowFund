using FlowFund.Api.Dtos;

namespace FlowFund.Api.Services
{
    public interface IUserService
    {
        Task<UserDto> GetMeAsync(int userId);
        Task<UserDto> UpdateProfileAsync(int userId, UpdateProfileDto dto);
        Task<UserDto> UpdateSettingsAsync(int userId, UpdateSettingsDto dto);
        Task<List<NotificationDto>> GetNotificationsAsync(int userId, bool unreadOnly);
        Task<NotificationDto> MarkNotificationReadAsync(int userId, int notificationId);
    }
}
