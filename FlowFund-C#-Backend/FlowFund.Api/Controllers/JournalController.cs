using FlowFund.Api.Dtos;
using FlowFund.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace FlowFund.Api.Controllers
{
    //Journal entry CRUD for the signed-in user.
    [Route("api/v1/journal")]
    public class JournalController : ApiControllerBase
    {
        private readonly IJournalService _journal;

        public JournalController(IJournalService journal) => _journal = journal;

        [HttpGet]
        public async Task<ActionResult<List<JournalEntryDto>>> List() => Ok(await _journal.ListAsync(CurrentUserId));

        [HttpGet("{id:int}")]
        public async Task<ActionResult<JournalEntryDto>> Get(int id) => Ok(await _journal.GetAsync(CurrentUserId, id));

        //POST journal - 201 when created, 200 when a replayed clientGeneratedId returns the stored entry
        [HttpPost]
        public async Task<ActionResult<JournalEntryDto>> Create([FromBody] SaveJournalEntryDto dto)
        {
            var (entry, created) = await _journal.CreateAsync(CurrentUserId, dto);
            return created ? Created($"api/v1/journal/{entry.Id}", entry) : Ok(entry);
        }

        [HttpPut("{id:int}")]
        public async Task<ActionResult<JournalEntryDto>> Update(int id, [FromBody] SaveJournalEntryDto dto) =>
            Ok(await _journal.UpdateAsync(CurrentUserId, id, dto));

        [HttpDelete("{id:int}")]
        public async Task<IActionResult> Delete(int id)
        {
            await _journal.DeleteAsync(CurrentUserId, id);
            return NoContent();
        }
    }
}
