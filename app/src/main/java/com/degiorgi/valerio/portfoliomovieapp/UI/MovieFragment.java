package com.degiorgi.valerio.portfoliomovieapp.UI;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.degiorgi.valerio.portfoliomovieapp.R;
import com.degiorgi.valerio.portfoliomovieapp.adapters.CursorMovieAdapter;
import com.degiorgi.valerio.portfoliomovieapp.data.MovieContentProvider;
import com.degiorgi.valerio.portfoliomovieapp.data.MovieDatabaseContract;
import com.degiorgi.valerio.portfoliomovieapp.models.MovieApiRequest;
import com.degiorgi.valerio.portfoliomovieapp.models.Result;
import com.degiorgi.valerio.portfoliomovieapp.retrofitInterface.MovieService;
import com.degiorgi.valerio.portfoliomovieapp.retrofitInterface.RetrofitServiceFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * Created by Valerio on 23/01/2016.
 */
public class MovieFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {


  public final static int LOADER_ID = 1;
  public CursorMovieAdapter mCursorAdapater;
  List<Result> Movies = new ArrayList<>();
  Call<MovieApiRequest> CallMovies;


  private void UpdateMovies() { //Executes the background Network Call


    MovieService.FetchMovieInterface MovieInterface = RetrofitServiceFactory.Factory().create(MovieService.FetchMovieInterface.class);


    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    String sortBy = preferences.getString(getString(R.string.sort_by_key), getString(R.string.sort_by_default));


    CallMovies = MovieInterface.getMovies(sortBy, getString(R.string.api_key));

    CallMovies.enqueue(new Callback<MovieApiRequest>() {

      @Override
      public void onResponse(Call<MovieApiRequest> call, Response<MovieApiRequest> response) {

        if (response != null) {

          MovieApiRequest request = response.body();
          if(request!=null){
          if (request.getResults() != null) {
            Movies = request.getResults();

            ContentResolver resolver = getActivity().getContentResolver();
            String[] args = {Movies.get(0).getId().toString()};

            Cursor cur = resolver.query(MovieContentProvider.Local_Movies.CONTENT_URI, null,
                MovieDatabaseContract.MovieId + "=?",
                args, null);

            if (cur != null) {
              if (!cur.moveToFirst()) {

                Vector<ContentValues> cVector = new Vector<>(Movies.size());

                for (int i = 0; i < Movies.size(); i++) {


                  ContentValues MovieValues = new ContentValues();

                  int id = Movies.get(i).getId();
                  String posterPath = Movies.get(i).getPosterPath();
                  String overview = Movies.get(i).getOverview();
                  String releaseDate = Movies.get(i).getReleaseDate();
                  String originalTitle = Movies.get(i).getOriginalTitle();
                  double voteAverage = Movies.get(i).getVoteAverage();
                  double popularity = Movies.get(i).getPopularity();


                  MovieValues.put(MovieDatabaseContract.MovieId, id);
                  MovieValues.put(MovieDatabaseContract.PosterUrl, posterPath);
                  MovieValues.put(MovieDatabaseContract.OriginalTitle, originalTitle);
                  MovieValues.put(MovieDatabaseContract.Overview, overview);
                  MovieValues.put(MovieDatabaseContract.ReleaseDate, releaseDate);
                  MovieValues.put(MovieDatabaseContract.UserRating, voteAverage);
                  MovieValues.put(MovieDatabaseContract.Popularity, popularity);


                  cVector.add(MovieValues);
                }

                int inserted = 0;
                if (cVector.size() > 0) {

                  ContentValues[] cArray = new ContentValues[cVector.size()];
                  cVector.toArray(cArray);
                  inserted = getActivity().getContentResolver().bulkInsert(MovieContentProvider.Local_Movies.CONTENT_URI, cArray);


                }

                Log.w("LOG TAG", "INSERTED" + inserted);


              }
              cur.close();
            }


          }
          }

        }
      }

      @Override
      public void onFailure(Call<MovieApiRequest> call, Throwable t) {

      }
    });

  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    getLoaderManager().initLoader(LOADER_ID, null, this);
  }

  @Override

  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


    mCursorAdapater = new CursorMovieAdapter(getActivity(), null, 0);
    View rootView = inflater.inflate(R.layout.movie_fragment_layout, container, false);

    GridView gridview = (GridView) rootView.findViewById(R.id.gridview_list);

    gridview.setAdapter(mCursorAdapater);


    gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Cursor cursor = (Cursor) parent.getItemAtPosition(position);

        if (cursor != null) {
          ((backCall) getActivity())
              .onGridItemSelected(MovieContentProvider.Local_Movies.withId(cursor.getInt(1)));
        }
      }
    });


    return rootView;

  }

  @Override
  public void onStop() {
    super.onStop();
    CallMovies.cancel();
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {

    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    String sortBy = preferences.getString(getString(R.string.sort_by_key), getString(R.string.sort_by_default));

    String sortOrder;

    if (sortBy.equals(getString(R.string.sort_by_popularity_value)) ||
        sortBy.equals(getString(R.string.sort_by_favourite_value))) {

      sortOrder = MovieDatabaseContract.Popularity + " DESC";

    } else {

      sortOrder = MovieDatabaseContract.UserRating + "  DESC";
    }

    if (sortBy.equals(getString(R.string.sort_by_favourite_value))) {

      return new CursorLoader(getActivity(), MovieContentProvider.Favourite_Movies.CONTENT_URI, null, null, null,
          sortOrder);
    } else {
      return new CursorLoader(getActivity(), MovieContentProvider.Local_Movies.CONTENT_URI,
          null, null, null, sortOrder);
    }
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    mCursorAdapater.swapCursor(data);
  }

  @Override
  public void onLoaderReset(Loader loader) {

    mCursorAdapater.swapCursor(null);
  }

  public void onSortChanged() {
    UpdateMovies();
    getLoaderManager().restartLoader(LOADER_ID, null, this);
  }

  public interface backCall {

    void onGridItemSelected(Uri contentUri);

  }


}




