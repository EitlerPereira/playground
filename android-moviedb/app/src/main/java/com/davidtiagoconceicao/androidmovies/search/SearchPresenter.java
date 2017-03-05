package com.davidtiagoconceicao.androidmovies.search;

import android.util.Log;
import android.util.LongSparseArray;

import com.davidtiagoconceicao.androidmovies.commons.MovieUtil;
import com.davidtiagoconceicao.androidmovies.data.Genre;
import com.davidtiagoconceicao.androidmovies.data.ImageConfiguration;
import com.davidtiagoconceicao.androidmovies.data.Movie;
import com.davidtiagoconceicao.androidmovies.data.remote.configuration.ConfigurationRepository;
import com.davidtiagoconceicao.androidmovies.data.remote.genre.GenresRemoteRepository;
import com.davidtiagoconceicao.androidmovies.data.remote.movie.MoviesRemoteRepository;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Presenter for upcoming movies list.
 * <p>
 * Created by david on 22/02/17.
 */

final class SearchPresenter implements SearchContract.Presenter {

    private final CompositeSubscription compositeSubscription;
    private final SearchContract.View view;
    private final MoviesRemoteRepository moviesRemoteRepository;
    private final GenresRemoteRepository genresRemoteRepository;
    private final ConfigurationRepository configurationRepository;

    private LongSparseArray<Genre> genres;
    private ImageConfiguration imageConfiguration;
    private final PublishSubject<String> searchSubject;

    SearchPresenter(
            SearchContract.View view,
            MoviesRemoteRepository moviesRemoteRepository,
            GenresRemoteRepository genresRemoteRepository,
            ConfigurationRepository configurationRepository) {

        this.view = view;
        this.moviesRemoteRepository = moviesRemoteRepository;
        this.genresRemoteRepository = genresRemoteRepository;
        this.configurationRepository = configurationRepository;
        this.compositeSubscription = new CompositeSubscription();

        searchSubject = PublishSubject.create();

        compositeSubscription.add(
                searchSubject
                        .debounce(300, TimeUnit.MILLISECONDS)
                        .flatMap(
                                new Func1<String, Observable<List<Movie>>>() {
                                    @Override
                                    public Observable<List<Movie>> call(String query) {
                                        return SearchPresenter.this.moviesRemoteRepository.searchMovie(query)
                                                .map(new Func1<Movie, Movie>() {
                                                    @Override
                                                    public Movie call(Movie movie) {
                                                        return MovieUtil.mapMovieFields(movie, imageConfiguration, genres);
                                                    }
                                                })
                                                .toList();
                                    }
                                })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<List<Movie>>() {
                            @Override
                            public void onCompleted() {
                                SearchPresenter.this.view.showLoading(false);
                            }

                            @Override
                            public void onError(Throwable e) {
                                SearchPresenter.this.handleException(e);
                            }

                            @Override
                            public void onNext(List<Movie> movies) {
                                SearchPresenter.this.view.showResult(movies);
                            }
                        }));

        this.view.setPresenter(this);
    }

    @Override
    public void onAttach() {
        //No action required
        view.showLoading(false);
    }

    @Override
    public void onDetach() {
        compositeSubscription.clear();
    }

    @Override
    public void search(final String query) {
        if (genres == null) {
            view.showLoading(true);

            genres = new LongSparseArray<>();

            loadGenres(query);

        } else {
            searchSubject.onNext(query);
        }
    }

    //Called by inner classes, default avoid accessors
    @SuppressWarnings("WeakerAccess")
    void handleException(Throwable e) {
        Log.e(getClass().getSimpleName(), "Exception on stream", e);
        view.showErrorLoading();
        view.showLoading(false);
    }

    //Called by inner classes, default avoid accessors
    @SuppressWarnings("WeakerAccess")
    void loadGenres(final String query) {
        compositeSubscription.add(
                genresRemoteRepository.getGenres()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Genre>() {

                            @Override
                            public void onCompleted() {
                                if (imageConfiguration == null) {
                                    loadImageConfiguration(query);
                                } else {
                                    searchSubject.onNext(query);
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                handleException(e);
                            }

                            @Override
                            public void onNext(Genre genre) {
                                genres.put(genre.id(), genre);
                            }
                        }));
    }

    //Called by inner classes, default avoid accessors
    @SuppressWarnings("WeakerAccess")
    void loadImageConfiguration(final String query) {
        compositeSubscription.add(
                configurationRepository.getImageConfiguration()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<ImageConfiguration>() {
                            @Override
                            public void onCompleted() {
                                searchSubject.onNext(query);
                            }

                            @Override
                            public void onError(Throwable e) {
                                handleException(e);
                            }

                            @Override
                            public void onNext(ImageConfiguration imageConfigurationResponse) {
                                imageConfiguration = imageConfigurationResponse;
                            }
                        }));
    }
}
