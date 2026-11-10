using FlowFund.Api.Dtos;
using FlowFund.Api.Models;

namespace FlowFund.Api.Services
{
    /// <summary>Single entity-to-DTO mapping for User, so AuthService and UserService can never drift apart.</summary>
    public static class UserMapper
    {
        public static UserDto ToDto(User u) => new()
        {
            Id = u.Id,
            FullName = u.FullName,
            Email = u.Email,
            BiometricEnabled = u.BiometricEnabled,
            PhoneNumber = u.PhoneNumber,
            LanguagePreference = u.LanguagePreference,
            Currency = string.IsNullOrWhiteSpace(u.Currency) ? "ZAR" : u.Currency
        };
    }
}
