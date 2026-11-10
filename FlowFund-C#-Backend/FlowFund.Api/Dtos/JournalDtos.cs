namespace FlowFund.Api.Dtos
{
    //Body of POST /journal and PUT /journal/{id}. entryDate is Unix epoch milliseconds (UTC).
    public class SaveJournalEntryDto
    {
        public string Title { get; set; } = string.Empty;
        public string Content { get; set; } = string.Empty;
        public string? Mood { get; set; }
        public long? EntryDate { get; set; }
        public Guid ClientGeneratedId { get; set; }
    }

    public class JournalEntryDto
    {
        public int Id { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Content { get; set; } = string.Empty;
        public string Mood { get; set; } = string.Empty;
        public long EntryDate { get; set; }
        public Guid ClientGeneratedId { get; set; }
    }
}
