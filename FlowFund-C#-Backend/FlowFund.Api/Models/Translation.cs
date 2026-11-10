using System.ComponentModel.DataAnnotations;
using FlowFund.Api.Enums;

namespace FlowFund.Api.Models
{
    public class Translation
    {
        public int Id { get; set; }
        public string ContentKey { get; set; } = string.Empty;
       
        public LanguageCode LanguageCode { get; set; }
        public string TranslatedText { get; set; } = string.Empty;
    }
}
