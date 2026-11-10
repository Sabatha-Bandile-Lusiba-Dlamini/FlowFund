using FlowFund.Api.Common;
using FlowFund.Api.Data;
using FlowFund.Api.Dtos;
using FlowFund.Api.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging.Abstractions;

namespace FlowFund.Api.Services
{
    //CRUD for a user's own journal entries. Same ownership and idempotency rules as TradeService.
    public class JournalService : IJournalService
    {
        private const string Tag = "JournalService";
        private readonly FlowFundDbContext _db;
        private readonly ILogger<JournalService> _log;

        public JournalService(FlowFundDbContext db, ILogger<JournalService>? log = null)
        {
            _db = db;
            _log = log ?? NullLogger<JournalService>.Instance;
        }

        public async Task<List<JournalEntryDto>> ListAsync(int userId)
        {
            var rows = await _db.JournalEntries.Where(e => e.UserId == userId)
                .OrderByDescending(e => e.EntryDate).ThenByDescending(e => e.Id).ToListAsync();
            return rows.Select(ToDto).ToList();
        }

        public async Task<JournalEntryDto> GetAsync(int userId, int entryId) => ToDto(await FindOwnedAsync(userId, entryId));

        public async Task<(JournalEntryDto Entry, bool Created)> CreateAsync(int userId, SaveJournalEntryDto dto)
        {
            var entryDate = Validate(dto);
            if (dto.ClientGeneratedId == Guid.Empty) throw new BadRequestException("clientGeneratedId is required.");

            var existing = await FindByClientIdAsync(userId, dto.ClientGeneratedId);
            if (existing is not null) return (ToDto(existing), false);

            var now = DateTime.UtcNow;
            var entry = new JournalEntry { UserId = userId, ClientGeneratedId = dto.ClientGeneratedId, CreatedAt = now };
            Apply(entry, dto, entryDate, now);

            _db.JournalEntries.Add(entry);
            try
            {
                await _db.SaveChangesAsync();
            }
            catch (DbUpdateException)
            {
                var winner = await FindByClientIdAsync(userId, dto.ClientGeneratedId);
                if (winner is null) throw;
                return (ToDto(winner), false);
            }

            _log.LogInformation("{Tag}: user {UserId} created journal entry {Id}", Tag, userId, entry.Id);
            return (ToDto(entry), true);
        }

        public async Task<JournalEntryDto> UpdateAsync(int userId, int entryId, SaveJournalEntryDto dto)
        {
            var entryDate = Validate(dto);
            var entry = await FindOwnedAsync(userId, entryId);
            Apply(entry, dto, entryDate, DateTime.UtcNow);
            await _db.SaveChangesAsync();
            _log.LogInformation("{Tag}: user {UserId} updated journal entry {Id}", Tag, userId, entryId);
            return ToDto(entry);
        }

        public async Task DeleteAsync(int userId, int entryId)
        {
            var entry = await FindOwnedAsync(userId, entryId);
            _db.JournalEntries.Remove(entry);
            await _db.SaveChangesAsync();
            _log.LogInformation("{Tag}: user {UserId} deleted journal entry {Id}", Tag, userId, entryId);
        }

        private async Task<JournalEntry> FindOwnedAsync(int userId, int entryId) =>
            await _db.JournalEntries.FirstOrDefaultAsync(e => e.Id == entryId && e.UserId == userId)
            ?? throw new NotFoundException("Journal entry not found.");

        private Task<JournalEntry?> FindByClientIdAsync(int userId, Guid clientId) =>
            _db.JournalEntries.FirstOrDefaultAsync(e => e.UserId == userId && e.ClientGeneratedId == clientId);

        private static DateTime Validate(SaveJournalEntryDto dto)
        {
            var title = dto.Title?.Trim() ?? string.Empty;
            if (title.Length is 0 or > 100) throw new BadRequestException("title must be 1-100 characters.");
            if (string.IsNullOrWhiteSpace(dto.Content)) throw new BadRequestException("content is required.");
            if (dto.Content.Length > 10000) throw new BadRequestException("content must be at most 10000 characters.");
            if ((dto.Mood?.Length ?? 0) > 30) throw new BadRequestException("mood must be at most 30 characters.");

            var entryDate = DateTime.UtcNow;
            if (dto.EntryDate is not null && !EpochTime.TryFromMillis(dto.EntryDate.Value, out entryDate))
                throw new BadRequestException("entryDate is not a valid epoch-millisecond timestamp.");
            return entryDate;
        }

        private static void Apply(JournalEntry e, SaveJournalEntryDto dto, DateTime entryDate, DateTime now)
        {
            e.Title = dto.Title.Trim();
            e.Content = dto.Content.Trim();
            e.Mood = dto.Mood?.Trim() ?? string.Empty;
            e.EntryDate = entryDate;
            e.UpdatedAt = now;
        }

        private static JournalEntryDto ToDto(JournalEntry e) => new()
        {
            Id = e.Id,
            Title = e.Title,
            Content = e.Content,
            Mood = e.Mood,
            EntryDate = EpochTime.ToMillis(e.EntryDate),
            ClientGeneratedId = e.ClientGeneratedId
        };
    }
}
