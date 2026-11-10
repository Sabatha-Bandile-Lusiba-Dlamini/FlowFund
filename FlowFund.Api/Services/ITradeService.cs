using FlowFund.Api.Dtos;

namespace FlowFund.Api.Services
{
    public interface ITradeService
    {
        Task<List<TradeDto>> ListAsync(int userId, DateTime? fromDate, DateTime? toDate);
        Task<TradeDto> GetAsync(int userId, int tradeId);
        Task<(TradeDto Trade, bool Created)> CreateAsync(int userId, SaveTradeDto dto);
        Task<TradeDto> UpdateAsync(int userId, int tradeId, SaveTradeDto dto);
        Task DeleteAsync(int userId, int tradeId);
        Task<TradeStatisticsDto> GetStatisticsAsync(int userId, DateTime? fromDate, DateTime? toDate);
    }
}
