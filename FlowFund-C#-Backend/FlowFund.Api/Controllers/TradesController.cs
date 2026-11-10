using FlowFund.Api.Dtos;
using FlowFund.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace FlowFund.Api.Controllers
{
    //Trade CRUD and statistics for the signed-in user. Business errors become 400/404 via the middleware.
    [Route("api/v1/trades")]
    public class TradesController : ApiControllerBase
    {
        private readonly ITradeService _trades;

        public TradesController(ITradeService trades) => _trades = trades;

        //GET trades[?from=&amp;to=] - newest first; from/to are ISO dates filtering on tradeDate
        [HttpGet]
        public async Task<ActionResult<List<TradeDto>>> List(
            [FromQuery(Name = "from")] DateTime? fromDate, [FromQuery(Name = "to")] DateTime? toDate) =>
            Ok(await _trades.ListAsync(CurrentUserId, fromDate, toDate));

        //GET trades/statistics[?from=&amp;to=] - totals, win rate and per-asset results from stored trades
        [HttpGet("statistics")]
        public async Task<ActionResult<TradeStatisticsDto>> Statistics(
            [FromQuery(Name = "from")] DateTime? fromDate, [FromQuery(Name = "to")] DateTime? toDate) =>
            Ok(await _trades.GetStatisticsAsync(CurrentUserId, fromDate, toDate));

        [HttpGet("{id:int}")]
        public async Task<ActionResult<TradeDto>> Get(int id) => Ok(await _trades.GetAsync(CurrentUserId, id));

        //POST trades - 201 when created, 200 when a replayed clientGeneratedId returns the stored trade
        [HttpPost]
        public async Task<ActionResult<TradeDto>> Create([FromBody] SaveTradeDto dto)
        {
            var (trade, created) = await _trades.CreateAsync(CurrentUserId, dto);
            return created ? Created($"api/v1/trades/{trade.Id}", trade) : Ok(trade);
        }

        [HttpPut("{id:int}")]
        public async Task<ActionResult<TradeDto>> Update(int id, [FromBody] SaveTradeDto dto) =>
            Ok(await _trades.UpdateAsync(CurrentUserId, id, dto));

        [HttpDelete("{id:int}")]
        public async Task<IActionResult> Delete(int id)
        {
            await _trades.DeleteAsync(CurrentUserId, id);
            return NoContent();
        }
    }
}
