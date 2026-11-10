namespace FlowFund.Api.Services
{
    public class TokenPair
    {
        public string AccessToken { get; set; } = string.Empty;
        public string RefreshToken { get; set; } = string.Empty;
    }

    public interface IJwtService
    {
        TokenPair GenerateTokens(int userId, string email);

        // Returns the principal only if the token is validly signed, unexpired and is a refresh token.
        System.Security.Claims.ClaimsPrincipal? ValidateRefreshToken(string token);
    }
}
