namespace FlowFund.Api.Models
{
    //One trade owned by one user. Mirrors the Android Room entity "trades"
    public class Trade
    {
        public int Id { get; set; }
        public int UserId { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Asset { get; set; } = string.Empty;

        //"BUY" or "SELL" (stored upper-case, exactly as the app sends it)
        public string TradeType { get; set; } = "BUY";

        public decimal EntryPrice { get; set; }
        public decimal ExitPrice { get; set; }
        public decimal Quantity { get; set; }
        public DateTime TradeDate { get; set; }
        public string Notes { get; set; } = string.Empty;

        //Idempotency key made on the phone: a retried sync must not create a second row
        public Guid ClientGeneratedId { get; set; }

        public DateTime CreatedAt { get; set; }
        public DateTime UpdatedAt { get; set; }
    }
}
