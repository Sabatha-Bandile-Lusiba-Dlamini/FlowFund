using FlowFund.Api.Dtos;

namespace FlowFund.Api.Services
{
    public interface IAuthService
    {
        Task<AuthResponseDto> VerifyAndIssueTokensAsync(string idToken);
        Task<AuthResponseDto> RefreshAsync(string refreshToken);
    }
}
