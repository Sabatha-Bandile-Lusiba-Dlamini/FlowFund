namespace FlowFund.Api.Services
{
    public class VerifiedTokenInfo
    {
        public string Uid { get; set; } = string.Empty;
        public string? Email { get; set; }
        public string? Name { get; set; }
    }
    public interface ITokenVerifier
    {
        Task<VerifiedTokenInfo> VerifyAsync(string idToken);
    }
}