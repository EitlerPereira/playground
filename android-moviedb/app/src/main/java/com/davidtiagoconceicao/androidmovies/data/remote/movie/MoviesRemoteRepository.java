package com.davidtiagoconceicao.androidmovies.data.remote.movie;

import android.util.Log;

import com.davidtiagoconceicao.androidmovies.commons.DateFormatUtil;
import com.davidtiagoconceicao.androidmovies.commons.retrofit.RetrofitServiceGenerator;
import com.davidtiagoconceicao.androidmovies.data.Movie;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Repository for movies operations.
 * <p>
 * Created by david on 22/02/17.
 */

public final class MoviesRemoteRepository {

    public Flowable<Movie> getUpcoming(int page) {
        return RetrofitServiceGenerator.generateService(MoviesEndpoint.class)
                .getUpcoming(page)
                .toFlowable()
                .flatMapIterable(new Function<MoviesQueryResponse, List<MovieResponse>>() {
                    @Override
                    public List<MovieResponse> apply(MoviesQueryResponse envelopeResponse) throws Exception {
                        return envelopeResponse.results();
                    }
                })
                .map(new Function<MovieResponse, Movie>() {
                    @Override
                    public Movie apply(MovieResponse movieResponse) throws Exception {
                        return mapMovie(movieResponse);
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    public Flowable<Movie> searchMovie(String query) {
        return RetrofitServiceGenerator.generateService(MoviesEndpoint.class)
                .getSearchResults(query)
                .toFlowable()
                .flatMapIterable(new Function<MoviesQueryResponse, List<MovieResponse>>() {
                    @Override
                    public List<MovieResponse> apply(MoviesQueryResponse envelopeResponse) throws Exception {
                        return envelopeResponse.results();
                    }
                })
                .map(new Function<MovieResponse, Movie>() {
                    @Override
                    public Movie apply(MovieResponse movieResponse) throws Exception {
                        return mapMovie(movieResponse);
                    }
                })
                .subscribeOn(Schedulers.io());
    }

    //Called by inner classes
    @SuppressWarnings("WeakerAccess")
    Movie mapMovie(MovieResponse movieResponse) {
        return Movie.builder()
                .title(movieResponse.title())
                .overview(movieResponse.overview())
                .posterPath(movieResponse.posterPath())
                .backdropPath(movieResponse.backdropPath())
                .releaseDate(
                        DateFormatUtil.parseDate(movieResponse.releaseDate()))
                .genreIds(movieResponse.genreIds())
                .build();
    }
}
