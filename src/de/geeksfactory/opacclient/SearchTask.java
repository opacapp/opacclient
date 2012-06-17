package de.geeksfactory.opacclient;

import java.util.List;

public class SearchTask extends OpacTask<List<SearchResult>> {

	@Override
	protected List<SearchResult> doInBackground(Object... arg0) {
        app = (OpacClient) arg0[0];
        String stichwort = (String) arg0[1];
        String verfasser = (String) arg0[2];
        String schlag_a = (String) arg0[3];
        String schlag_b = (String) arg0[4];
        String zweigstelle = (String) arg0[5];
        String mediengruppe = (String) arg0[6];
        String isbn = (String) arg0[7];
        String jahr_von = (String) arg0[8];
        String jahr_bis = (String) arg0[9];
        String notation = (String) arg0[10];
        String interessenkreis = (String) arg0[11];
        String verlag = (String) arg0[12];
        String order = (String) arg0[13];
        
        try {
			return app.ohc.search(stichwort, verfasser, schlag_a, schlag_b, zweigstelle, 
					mediengruppe, isbn, jahr_von, jahr_bis, notation, interessenkreis, verlag, order);
		} catch (Exception e) {
			publishProgress(e, "ioerror");
		}
        
		return null;
	}
	
    protected void onPostExecute(List<SearchResult> result) {
    }

}
