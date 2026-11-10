namespace FlowFund.Api.Models
{
    //One journal entry owned by one user. Mirrors the Android Room entity "journal_entries"
    public class JournalEntry
    {
        public int Id { get; set; }
        public int UserId { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Content { get; set; } = string.Empty;
        public string Mood { get; set; } = string.Empty;
        public DateTime EntryDate { get; set; }
        public Guid ClientGeneratedId { get; set; }
        public DateTime CreatedAt { get; set; }
        public DateTime UpdatedAt { get; set; }
    }
}
