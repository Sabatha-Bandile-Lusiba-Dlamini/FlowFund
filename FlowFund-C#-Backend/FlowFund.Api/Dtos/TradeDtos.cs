namespace FlowFund.Api.Dtos
{
  
    // Body of POST /trades and PUT /trades/{id}. Dates travel as Unix epoch MILLISECONDS (UTC), the same
    //representation the Android Room database stores, so neither side has to parse date strings.
 
    public class SaveTradeDto
    {
        public string Title { get; set; } = string.Empty;
        public string Asset { get; set; } = string.Empty;
        public string TradeType { get; set; } = string.Empty;
        public decimal EntryPrice { get; set; }
        public decimal ExitPrice { get; set; }
        public decimal Quantity { get; set; }
        public long? TradeDate { get; set; }
        public string? Notes { get; set; }
        public Guid ClientGeneratedId { get; set; }
    }

    public class TradeDto
    {
        public int Id { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Asset { get; set; } = string.Empty;
        public string TradeType { get; set; } = string.Empty;
        public decimal EntryPrice { get; set; }
        public decimal ExitPrice { get; set; }
        public decimal Quantity { get; set; }
        public long TradeDate { get; set; }
        public string Notes { get; set; } = string.Empty;
        public Guid ClientGeneratedId { get; set; }

        //Computed by TradeCalculator on every read; never stored
        public decimal ProfitLoss { get; set; }
    }

    public class AssetStatisticsDto
    {
        public string Asset { get; set; } = string.Empty;
        public int Trades { get; set; }
        public decimal ProfitLoss { get; set; }
    }

    //GET /trades/statistics. Every figure is calculated from the caller's stored trades
    public class TradeStatisticsDto
    {
        public int TotalClosedTrades { get; set; }
        public int WinningTrades { get; set; }
        public int LosingTrades { get; set; }
        public decimal TotalProfitLoss { get; set; }

        //Percentage 0-100: winning trades / all closed trades (same formula as the Android app)
        public double WinRate { get; set; }
        public List<AssetStatisticsDto> ByAsset { get; set; } = new();
    }
}
