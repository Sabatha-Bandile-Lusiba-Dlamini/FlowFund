using FlowFund.Api.Dtos;

namespace FlowFund.Api.Services
{
    // Pure trade maths: no database, no clock. It is the server twin of the Android calculateTradeProfitLoss 
    // calculateAnalytics functions, so the app and API report identical numbers. Unit tests call it directly.
    
    public static class TradeCalculator
    {
        //BUY: (exit - entry) x qty. SELL: (entry - exit) x qty. Anything else: 0.
        public static decimal ProfitLoss(string tradeType, decimal entryPrice, decimal exitPrice, decimal quantity) =>
            (tradeType ?? string.Empty).Trim().ToUpperInvariant() switch
            {
                "BUY" => (exitPrice - entryPrice) * quantity,
                "SELL" => (entryPrice - exitPrice) * quantity,
                _ => 0m
            };

        //Wins are P/L &gt; 0, losses P/L &lt; 0; break-even trades count only in the total.
        public static TradeStatisticsDto Summarise(IEnumerable<(string Asset, decimal ProfitLoss)> results)
        {
            var list = results.ToList();
            var wins = list.Count(r => r.ProfitLoss > 0m);
            var losses = list.Count(r => r.ProfitLoss < 0m);

            return new TradeStatisticsDto
            {
                TotalClosedTrades = list.Count,
                WinningTrades = wins,
                LosingTrades = losses,
                TotalProfitLoss = list.Sum(r => r.ProfitLoss),
                WinRate = list.Count == 0 ? 0.0 : (double)wins / list.Count * 100.0,
                ByAsset = list.GroupBy(r => r.Asset)
                    .Select(g => new AssetStatisticsDto { Asset = g.Key, Trades = g.Count(), ProfitLoss = g.Sum(r => r.ProfitLoss) })
                    .OrderByDescending(a => a.ProfitLoss)
                    .ToList()
            };
        }
    }
}
