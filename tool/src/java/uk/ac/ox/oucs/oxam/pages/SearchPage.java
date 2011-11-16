package uk.ac.ox.oucs.oxam.pages;

import java.util.Iterator;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.wicket.PageParameters;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class SearchPage extends WebPage {

	@SpringBean(name="solrServer")
	private SolrServer solr;

	private TextField<String> query;

	// query string, facet restrictions (parsed), current page, 
	// AbstractPageableView to manage this, getItemModels() needs to call through to Solr.

	class SolrProvider implements IDataProvider<SolrDocument>
	{
		private static final long serialVersionUID = 1L;
		private int first = 0;
		private int count = 10;
		private transient QueryResponse response;

		public void detach() {
			// TODO Auto-generated method stub

		}

		public Iterator<SolrDocument> iterator(int first, int count) {
			this.first = first;
			this.count = count;
			doSearch();
			return response.getResults().iterator();
		}

		public int size() {
			if (response == null) {
				doSearch();
			}
			return (int) response.getResults().getNumFound();
		}

		public IModel<SolrDocument> model(SolrDocument object) {
			return new CompoundPropertyModel<SolrDocument>(object);
		}

		private void doSearch() {
			SolrQuery solrQuery = new SolrQuery().
					setQuery(query.getValue().length() > 0?query.getValue():"*:*").
					setStart(first).
					setRows(count).
					setFacet(true).
					setFacetMinCount(1).
					setFacetLimit(10).
					addFacetField("exam_code", "paper_code", "year", "term");

			try {
				response = solr.query(solrQuery);
			} catch (SolrServerException sse) {
				// TODO put error for something.
			}
		}
		
		public ListView<Count> getFacet(String id, String facet) {
			if (response == null) {
				doSearch();
			}
			return new ListView<Count>(id, response.getFacetField(facet).getValues()) {
				private static final long serialVersionUID = 1L;
				@Override
				protected void populateItem(ListItem<Count> item) {
					Count count = item.getModelObject();
					item.add(new Label("name", count.getName()));
					item.add(new Label("count", Long.toString(count.getCount())));
				}
			};
		}

	}


	public SearchPage(final PageParameters p) {
		query = new TextField<String>("query", new Model<String>(p.getString("query")));

		SolrProvider provider = new SolrProvider();

		DataView<SolrDocument> dataView = new DataView<SolrDocument>("results", provider)  {
			private static final long serialVersionUID = 1L;

			public void populateItem(final Item<SolrDocument> item)
			{
				SolrDocument document = item.getModelObject();
				ExternalLink link = new ExternalLink("paper_file", document.getFieldValue("paper_file").toString());
				item.add(link);
				link.add(new Label("paper_code", document.getFieldValue("paper_code").toString()));
				link.add(new Label("paper_title", document.getFieldValue("paper_title").toString()));
				item.add(new Label("year", document.getFieldValue("year").toString()));
				item.add(new Label("term", document.getFieldValue("term").toString()));
				item.add(new Label("exam_code", document.getFieldValue("exam_code").toString()));
				item.add(new Label("exam_title", document.getFieldValue("exam_title").toString()));
			}
		};
		dataView.setItemsPerPage(10);
		add(dataView);
		add(new PagingNavigator("resultsNavigation", dataView));
		add(new NavigatorLabel("resultsLabel", dataView));
		
		add(provider.getFacet("paper_code_facet", "paper_code"));
		add(provider.getFacet("exam_code_facet", "exam_code"));
		add(provider.getFacet("year_facet", "year"));
		add(provider.getFacet("term_facet", "term"));

		setStatelessHint(true);
		query.setRequired(true);
		final StatelessForm<?> form = new StatelessForm("searchForm") {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit() {
				System.out.println(query.getValue());
				p.put("query", query.getValue());

				setResponsePage(SearchPage.class, p);
			}

			@Override
			protected String getMethod() {
				return "get";
			}

		};
		form.add(query);
		form.setRedirect(false);
		add(form);
	}

}
