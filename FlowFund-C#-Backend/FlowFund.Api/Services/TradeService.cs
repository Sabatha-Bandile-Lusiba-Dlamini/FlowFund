using FlowFund.Api.Common;
using FlowFund.Api.Data;
using FlowFund.Api.Dtos;
using FlowFund.Api.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace FlowFund.Api.Services
{
  
    //CRUD and statistics for a user's own trades. Every query filters on UserId, so one user can never read or
    //change another's trade; someone else's id simply behaves as "not found" (no existence leak).
  
    public class TradeService : ITradeService
    {
        private const string Tag = "TradeService";
        private readonly FlowFundDbContext _db;
        private readonly ILogger<TradeService> _log;

        public TradeService(FlowFundDbContext db, ILogger<TradeService>? log = null)
        {
            _db = db;
            _log = log ?? NullLogger<TradeService>.Instance;
        }

        public async Task<List<TradeDto>> ListAsync(int userId, DateTime? fromDate, DateTime? toDate)
        {
            var rows = await Query(userId, fromDate, toDate).OrderByDescending(t => t.TradeDate).ThenByDescending(t => t.Id).ToListAsync();
            return rows.Select(ToDto).ToList();
        }

        public async Task<TradeDto> GetAsync(int userId, int tradeId) => ToDto(await FindOwnedAsync(userId, tradeId));

        public async Task<(TradeDto Trade, bool Created)> CreateAsync(int userId, SaveTradeDto dto)
        {
            var (type, tradeDate) = Validate(dto);
            if (dto.ClientGeneratedId == Guid.Empty) throw new BadRequestException("clientGeneratedId is required.");

            var existing = await FindByClientIdAsync(userId, dto.ClientGeneratedId);
            if (existing is not null)
            {
                _log.LogInformation("{Tag}: replayed client id for trade {Id}; returning stored row", Tag, existing.Id);
                return (ToDto(existing), false);
            }

            var now = DateTime.UtcNow;
            var trade = new Trade { UserId = userId, ClientGeneratedId = dto.ClientGeneratedId, CreatedAt = now };
            Apply(trade, dto, type, tradeDate, now);

            _db.Trades.Add(trade);
            try
            {
                await _db.SaveChangesAsync();
            }
            catch (DbUpdateException)
            {
                // A concurrent retry won the race against the unique (UserId, ClientGeneratedId) index.
                var winner = await FindByClientIdAsync(userId, dto.ClientGeneratedId);
                if (winner is null) throw;
                return (ToDto(winner), false);
            }

            _log.LogInformation("{Tag}: user {UserId} created trade {Id} ({Asset})", Tag, userId, trade.Id, trade.Asset);
            return (ToDto(trade), true);
        }

        public async Task<TradeDto> UpdateAsync(int userId, int tradeId, SaveTradeDto dto)
        {
            var (type, tradeDate) = Validate(dto);
            var trade = await FindOwnedAsync(userId, tradeId);
            Apply(trade, dto, type, tradeDate, DateTime.UtcNow);
            await _db.SaveChangesAsync();
            _log.LogInformation("{Tag}: user {UserId} updated trade {Id}", Tag, userId, tradeId);
            return ToDto(trade);
        }

        public async Task DeleteAsync(int userId, int tradeId)
        {
            var trade = await FindOwnedAsync(userId, tradeId);
            _db.Trades.Remove(trade);
            await _db.SaveChangesAsync();
            _log.LogInformation("{Tag}: user {UserId} deleted trade {Id}", Tag, userId, tradeId);
        }

        public async Task<TradeStatisticsDto> GetStatisticsAsync(int userId, DateTime? fromDate, DateTime? toDate)
        {
            var rows = await Query(userId, fromDate, toDate).ToListAsync();
            var stats = TradeCalculator.Summarise(rows.Select(t =>
                (t.Asset, TradeCalculator.ProfitLoss(t.TradeType, t.EntryPrice, t.ExitPrice, t.Quantity))));
            _log.LogInformation("{Tag}: statistics for user {UserId} over {Count} trades", Tag, userId, rows.Count);
            return stats;
        }

        // helpers 
        private IQueryable<Trade> Query(int userId, DateTime? fromDate, DateTime? toDate) =>
            _db.Trades.Where(t => t.UserId == userId
                                  && (fromDate == null || t.TradeDate >= fromDate)
                                  && (toDate == null || t.TradeDate <= toDate));

        private async Task<Trade> FindOwnedAsync(int userId, int tradeId) =>
            await _db.Trades.FirstOrDefaultAsync(t => t.Id == tradeId && t.UserId == userId)
            ?? throw new NotFoundException("Trade not found.");

        private Task<Trade?> FindByClientIdAsync(int userId, Guid clientId) =>
            _db.Trades.FirstOrDefaultAsync(t => t.UserId == userId && t.ClientGeneratedId == clientId);

        //Validates every field before anything is saved; returns the normalised type and the trade date
        private static (string Type, DateTime TradeDate) Validate(SaveTradeDto dto)
        {
            var title = dto.Title?.Trim() ?? string.Empty;
            var asset = dto.Asset?.Trim() ?? string.Empty;
            var type = dto.TradeType?.Trim().ToUpperInvariant() ?? string.Empty;

            if (title.Length is 0 or > 100) throw new BadRequestException("title must be 1-100 characters.");
            if (asset.Length is 0 or > 30) throw new BadRequestException("asset must be 1-30 characters.");
            if (type is not ("BUY" or "SELL")) throw new BadRequestException("tradeType must be BUY or SELL.");
            if (dto.EntryPrice < 0 || dto.ExitPrice < 0) throw new BadRequestException("Prices cannot be negative.");
            if (dto.Quantity <= 0) throw new BadRequestException("quantity must be greater than zero.");
            if ((dto.Notes?.Length ?? 0) > 2000) throw new BadRequestException("notes must be at most 2000 characters.");

            var tradeDate = DateTime.UtcNow;
            if (dto.TradeDate is not null && !EpochTime.TryFromMillis(dto.TradeDate.Value, out tradeDate))
                throw new BadRequestException("tradeDate is not a valid epoch-millisecond timestamp.");

            return (type, tradeDate);
        }

        private static void Apply(Trade t, SaveTradeDto dto, string type, DateTime tradeDate, DateTime now)
        {
            t.Title = dto.Title.Trim();
            t.Asset = dto.Asset.Trim();
            t.TradeType = type;
            t.EntryPrice = dto.EntryPrice;
            t.ExitPrice = dto.ExitPrice;
            t.Quantity = dto.Quantity;
            t.TradeDate = tradeDate;
            t.Notes = dto.Notes?.Trim() ?? string.Empty;
            t.UpdatedAt = now;
        }

        private static TradeDto ToDto(Trade t) => new()
        {
            Id = t.Id,
            Title = t.Title,
            Asset = t.Asset,
            TradeType = t.TradeType,
            EntryPrice = t.EntryPrice,
            ExitPrice = t.ExitPrice,
            Quantity = t.Quantity,
            TradeDate = EpochTime.ToMillis(t.TradeDate),
            Notes = t.Notes,
            ClientGeneratedId = t.ClientGeneratedId,
            ProfitLoss = TradeCalculator.ProfitLoss(t.TradeType, t.EntryPrice, t.ExitPrice, t.Quantity)
        };
    }
}
