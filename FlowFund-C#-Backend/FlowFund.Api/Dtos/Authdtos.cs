using FlowFund.Api.Enums;
namespace FlowFund.Api.Dtos
{

    // The request body for POST /api/v1/auth/sso/verify — exactly one field,
    // matching what the brief specifies the endpoint accepts.
    public class SsoVerifyRequest
    {
        public string IdToken { get; set; } = string.Empty;
    }

    // What the endpoint hands back: the JWT access/refresh pair the brief calls for,
    // plus enough user info for the app to render a profile without a second call.
    public class AuthResponseDto
    {
        public string AccessToken { get; set; } = string.Empty;
        public string RefreshToken { get; set; } = string.Empty;
        public UserDto User { get; set; } = null!;
    }

    // A DTO, not the raw User entity — so the API's public contract doesn't
    // change every time the database schema does, and so internal fields
    // (like FirebaseUid) never leak into a response body.
    public class UserDto
    {
        public int Id { get; set; }
        public string FullName { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public bool BiometricEnabled { get; set; }
        public string PhoneNumber { get; set; } = string.Empty;
        public LanguageCode LanguagePreference { get; set; }
        public string Currency { get; set; } = "ZAR";
    }

    // Body of POST /api/v1/auth/refresh.
    public class RefreshRequest
    {
        public string RefreshToken { get; set; } = string.Empty;
    }
}