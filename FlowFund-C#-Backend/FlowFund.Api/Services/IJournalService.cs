using FlowFund.Api.Dtos;

namespace FlowFund.Api.Services
{
    public interface IJournalService
    {
        Task<List<JournalEntryDto>> ListAsync(int userId);
        Task<JournalEntryDto> GetAsync(int userId, int entryId);
        Task<(JournalEntryDto Entry, bool Created)> CreateAsync(int userId, SaveJournalEntryDto dto);
        Task<JournalEntryDto> UpdateAsync(int userId, int entryId, SaveJournalEntryDto dto);
        Task DeleteAsync(int userId, int entryId);
    }
}
