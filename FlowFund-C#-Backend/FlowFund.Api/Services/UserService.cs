using System.Text.RegularExpressions;
using FlowFund.Api.Common;
using FlowFund.Api.Data;
using FlowFund.Api.Dtos;
using FlowFund.Api.Enums;
using FlowFund.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace FlowFund.Api.Services
{
    /// <summary>Profile, settings (REQ-SET-01/02/04, REQ-BIO-03) and the in-app notification feed (REQ-NOTIF-04).</summary>
    public class UserService : IUserService
    {
        private const string Tag = "UserService";
        private static readonly Regex CurrencyCode = new("^[A-Za-z]{3}$", RegexOptions.Compiled);
        private readonly FlowFundDbContext _db;
        private readonly ILogger<UserService> _log;

        public UserService(FlowFundDbContext db, ILogger<UserService> log)
        {
            _db = db;
            _log = log;
        }

        public async Task<UserDto> GetMeAsync(int userId) => UserMapper.ToDto(await GetUserAsync(userId));

        public async Task<UserDto> UpdateProfileAsync(int userId, UpdateProfileDto dto)
        {
            var user = await GetUserAsync(userId);

            if (dto.FullName is not null)
            {
                var name = dto.FullName.Trim();
                if (name.Length == 0 || name.Length > 100) throw new BadRequestException("fullName must be 1-100 characters.");
                user.FullName = name;
            }
            if (dto.PhoneNumber is not null)
            {
                var phone = dto.PhoneNumber.Trim();
                if (phone.Length > 20) throw new BadRequestException("phoneNumber must be at most 20 characters.");
                user.PhoneNumber = phone;
            }

            await _db.SaveChangesAsync();
            _log.LogInformation("{Tag}: user {UserId} updated profile", Tag, userId);
            return UserMapper.ToDto(user);
        }

        /// <summary>Validates every supplied field BEFORE changing any, so a bad request never half-applies.</summary>
        public async Task<UserDto> UpdateSettingsAsync(int userId, UpdateSettingsDto dto)
        {
            var user = await GetUserAsync(userId);

            if (dto.LanguagePreference is not null && !Enum.IsDefined(dto.LanguagePreference.Value))
                throw new BadRequestException("languagePreference must be one of En, Af, Zu.");
            if (dto.Currency is not null && !CurrencyCode.IsMatch(dto.Currency.Trim()))
                throw new BadRequestException("currency must be a 3-letter ISO 4217 code, e.g. ZAR.");

            if (dto.LanguagePreference is not null) user.LanguagePreference = dto.LanguagePreference.Value;
            if (dto.Currency is not null) user.Currency = dto.Currency.Trim().ToUpperInvariant();
            if (dto.BiometricEnabled is not null) user.BiometricEnabled = dto.BiometricEnabled.Value;

            await _db.SaveChangesAsync();
            _log.LogInformation("{Tag}: user {UserId} updated settings", Tag, userId);
            return UserMapper.ToDto(user);
        }

        public async Task<List<NotificationDto>> GetNotificationsAsync(int userId, bool unreadOnly)
        {
            // Notification has no timestamp column, so newest = highest Id (identity values only grow).
            var rows = await _db.Notifications
                .Where(n => n.UserId == userId && (!unreadOnly || !n.IsRead))
                .OrderByDescending(n => n.Id)
                .ToListAsync();
            return rows.Select(ToDto).ToList();
        }

        public async Task<NotificationDto> MarkNotificationReadAsync(int userId, int notificationId)
        {
            // Filtering on UserId as well means you cannot mark (or even learn about) someone else's notification.
            var n = await _db.Notifications.FirstOrDefaultAsync(x => x.Id == notificationId && x.UserId == userId)
                    ?? throw new NotFoundException("Notification not found.");
            n.IsRead = true;
            await _db.SaveChangesAsync();
            return ToDto(n);
        }

        private async Task<User> GetUserAsync(int userId) =>
            await _db.Users.FirstOrDefaultAsync(u => u.Id == userId)
            ?? throw new NotFoundException("User not found.");

        private static NotificationDto ToDto(Notification n) => new()
        {
            Id = n.Id, Message = n.Message, Type = n.NotificationType, IsRead = n.IsRead
        };
    }
}
