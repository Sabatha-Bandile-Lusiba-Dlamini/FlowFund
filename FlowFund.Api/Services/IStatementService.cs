using FlowFund.Api.Dtos;

namespace FlowFund.Api.Services
{
    public interface IStatementService
    {
        Task<StatementDto> GetMyStatementAsync(int userId, int? groupId, DateTime? fromDate, DateTime? toDate, DateTime asOf);
    }
}
