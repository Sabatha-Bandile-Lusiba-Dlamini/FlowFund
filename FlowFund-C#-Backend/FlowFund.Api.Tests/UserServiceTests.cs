using FlowFund.Api.Common;
using FlowFund.Api.Dtos;
using FlowFund.Api.Enums;
using FlowFund.Api.Models;
using FlowFund.Api.Services;
using Microsoft.Extensions.Logging.Abstractions;
using Xunit;

namespace FlowFund.Api.Tests
{
    public class UserServiceTests
    {
        private static UserService NewService(FlowFund.Api.Data.FlowFundDbContext db) =>
            new(db, NullLogger<UserService>.Instance);

        [Fact]
        public async Task UpdateSettings_ChangesOnlySentFields()
        {
            using var db = TestSupport.NewDb(nameof(UpdateSettings_ChangesOnlySentFields));
            var user = await TestSupport.AddUserAsync(db);

            var result = await NewService(db).UpdateSettingsAsync(user.Id, new UpdateSettingsDto { LanguagePreference = LanguageCode.Zu });

            Assert.Equal(LanguageCode.Zu, result.LanguagePreference);
            Assert.Equal("ZAR", result.Currency);          // untouched
            Assert.False(result.BiometricEnabled);         // untouched
        }

        [Theory]
        [InlineData("RAND")]
        [InlineData("R1")]
        [InlineData("")]
        public async Task UpdateSettings_InvalidCurrency_ThrowsBadRequest(string currency)
        {
            using var db = TestSupport.NewDb($"{nameof(UpdateSettings_InvalidCurrency_ThrowsBadRequest)}{currency}");
            var user = await TestSupport.AddUserAsync(db);

            await Assert.ThrowsAsync<BadRequestException>(() =>
                NewService(db).UpdateSettingsAsync(user.Id, new UpdateSettingsDto { Currency = currency }));
        }

        [Fact]
        public async Task UpdateSettings_LowercaseCurrency_IsNormalisedToUpper()
        {
            using var db = TestSupport.NewDb(nameof(UpdateSettings_LowercaseCurrency_IsNormalisedToUpper));
            var user = await TestSupport.AddUserAsync(db);

            var result = await NewService(db).UpdateSettingsAsync(user.Id, new UpdateSettingsDto { Currency = "usd" });

            Assert.Equal("USD", result.Currency);
        }

        [Fact]
        public async Task MarkNotificationRead_OtherUsersNotification_ThrowsNotFound()
        {
            using var db = TestSupport.NewDb(nameof(MarkNotificationRead_OtherUsersNotification_ThrowsNotFound));
            var owner = await TestSupport.AddUserAsync(db);
            var other = await TestSupport.AddUserAsync(db);
            db.Notifications.Add(new Notification { UserId = owner.Id, Message = "hi", NotificationType = NotificationType.Confirmation });
            await db.SaveChangesAsync();
            var id = db.Notifications.Single().Id;
            var service = NewService(db);

            await Assert.ThrowsAsync<NotFoundException>(() => service.MarkNotificationReadAsync(other.Id, id));
            await service.MarkNotificationReadAsync(owner.Id, id);

            Assert.True(db.Notifications.Single().IsRead);
        }

        [Fact]
        public async Task GetNotifications_ReturnsOnlyCallersRows_NewestFirst()
        {
            using var db = TestSupport.NewDb(nameof(GetNotifications_ReturnsOnlyCallersRows_NewestFirst));
            var me = await TestSupport.AddUserAsync(db);
            var other = await TestSupport.AddUserAsync(db);
            db.Notifications.Add(new Notification { UserId = me.Id, Message = "old" });
            db.Notifications.Add(new Notification { UserId = other.Id, Message = "not mine" });
            db.Notifications.Add(new Notification { UserId = me.Id, Message = "new" });
            await db.SaveChangesAsync();

            var list = await NewService(db).GetNotificationsAsync(me.Id, unreadOnly: false);

            Assert.Equal(new[] { "new", "old" }, list.Select(n => n.Message));
        }
    }
}
