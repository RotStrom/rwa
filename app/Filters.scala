import javax.inject.Inject
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.gzip.GzipFilter
import play.filters.headers.SecurityHeadersFilter
import filters.ScoreFilter

class Filters @Inject() (gzip: GzipFilter) extends HttpFilters {
  val filters: Seq[EssentialFilter] = Seq(gzip, SecurityHeadersFilter(), new ScoreFilter())
}
