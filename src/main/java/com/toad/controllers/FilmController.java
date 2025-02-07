package com.toad.controllers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.CrossOrigin;

import com.toad.entities.Film;
import com.toad.repositories.FilmRepository;
import com.toad.utils.UniqueIdGenerator;

@Controller
@RequestMapping(path = "/toad/film")
@CrossOrigin(origins = "http://localhost:8000")
public class FilmController {
    @Autowired
    private FilmRepository filmRepository;

    @PostMapping(path = "/add")
    public @ResponseBody String addNewFilm(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Integer releaseYear,
            @RequestParam Byte languageId,
            @RequestParam Byte originalLanguageId,
            @RequestParam Byte rentalDuration,
            @RequestParam Double rentalRate,
            @RequestParam Integer length,
            @RequestParam Double replacementCost,
            @RequestParam String rating,
            @RequestParam String lastUpdate, // Changement ici pour String
            @RequestParam Long idDirector) {

        Film film = new Film();
        film.setTitle(title);
        film.setDescription(description);
        film.setReleaseYear(releaseYear);
        film.setLanguageId(languageId);
        film.setOriginalLanguageId(originalLanguageId);
        film.setRentalDuration(rentalDuration);
        film.setRentalRate(rentalRate);
        film.setLength(length);
        film.setReplacementCost(replacementCost);
        film.setRating(rating);
        
        // Vérification et conversion de la date
        try {
            film.setLastUpdate(java.sql.Timestamp.valueOf(lastUpdate.replace("Z", "+00:00"))); // Conversion ici
        } catch (IllegalArgumentException e) {
            return "Erreur: Format de date invalide. Doit être yyyy-mm-dd hh:mm:ss[.fffffffff]";
        }
        
        film.setIdDirector(idDirector);
        
        System.out.println(film.getTitle());

        filmRepository.save(film);
        return "Film Sauvegardé";
    }

    @GetMapping(path = "/all")
    public @ResponseBody Iterable<Film> getAllFilms() {
        return filmRepository.findAll();
    }

    @GetMapping(path = "/getById")
    public @ResponseBody Film getFilmById(@RequestParam Integer id) {
        Film film = filmRepository.findById(id).orElse(null);
        if (film != null) {
            Film filteredFilm = new Film();
            filteredFilm.setTitle(film.getTitle());
            filteredFilm.setDescription(film.getDescription());
            filteredFilm.setReleaseYear(film.getReleaseYear());
            filteredFilm.setRentalDuration(film.getRentalDuration());
            filteredFilm.setRentalRate(film.getRentalRate());
            filteredFilm.setRating(film.getRating());
            return filteredFilm;
        }
        return null;
    }

    @PutMapping(path = "/update/{id}")
    public @ResponseBody String updateFilm(
            @PathVariable Integer id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Integer releaseYear,
            @RequestParam Byte languageId,
            @RequestParam Byte originalLanguageId,
            @RequestParam Byte rentalDuration,
            @RequestParam Double rentalRate,
            @RequestParam Integer length,
            @RequestParam Double replacementCost,
            @RequestParam String rating,
            @RequestParam java.sql.Timestamp lastUpdate,
            @RequestParam Long idDirector) {

        String status = null;
        if (filmRepository.existsById(id)) {
            Film film = filmRepository.findById(id).orElse(null);
            film.setTitle(title);
            film.setDescription(description);
            film.setReleaseYear(releaseYear);
            film.setLanguageId(languageId);
            film.setOriginalLanguageId(originalLanguageId);
            film.setRentalDuration(rentalDuration);
            film.setRentalRate(rentalRate);
            film.setLength(length);
            film.setReplacementCost(replacementCost);
            film.setRating(rating);
            film.setLastUpdate(lastUpdate);
            film.setIdDirector(idDirector);

            filmRepository.save(film);
            status = "Film Mis à jour";
        } else {
            status = "Film non trouvé";
        }
        return status;
    }

    @DeleteMapping(path = "/delete/{id}")
    public @ResponseBody String deleteFilm(@PathVariable Integer id) {
        String status = null;
        if (filmRepository.existsById(id)) {
            filmRepository.deleteById(id);
            status = "Film supprimé";
        } else {
            status = "Film pas trouvé";
        }
        return status;
    }

    @GetMapping(path = "/page")
    public @ResponseBody HashMap<String, Object> getFilmsByPage(
            @RequestParam int page,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer start_year,
            @RequestParam(required = false) Integer end_year) {
        
        int pageSize = 9;
        int offset = page * pageSize;
        
        List<Film> allFilms = (List<Film>) filmRepository.findAll();
        
        // Filtrer les films
        List<Film> filteredFilms = allFilms.stream()
                .filter(film -> {
                    boolean matchesSearch = search == null || 
                        film.getTitle().toLowerCase().contains(search.toLowerCase());
                    boolean matchesStartYear = start_year == null || 
                        film.getReleaseYear() >= start_year;
                    boolean matchesEndYear = end_year == null || 
                        film.getReleaseYear() <= end_year;
                    return matchesSearch && matchesStartYear && matchesEndYear;
                })
                .sorted((f1, f2) -> {
                    if ("title_asc".equals(sort)) {
                        return f1.getTitle().compareTo(f2.getTitle());
                    } else if ("title_desc".equals(sort)) {
                        return f2.getTitle().compareTo(f1.getTitle());
                    } else if ("year_asc".equals(sort)) {
                        return f1.getReleaseYear().compareTo(f2.getReleaseYear());
                    } else if ("year_desc".equals(sort)) {
                        return f2.getReleaseYear().compareTo(f1.getReleaseYear());
                    }
                    return 0;
                })
                .collect(Collectors.toList());

        // Calculer les indices de début et fin pour la pagination
        int startIndex = Math.min(offset, filteredFilms.size());
        int endIndex = Math.min(offset + pageSize, filteredFilms.size());
        
        // Préparer la réponse
        HashMap<String, Object> response = new HashMap<>();
        response.put("total", filteredFilms.size());
        response.put("totalPages", (int) Math.ceil(filteredFilms.size() / (double) pageSize));
        response.put("currentPage", page);
        response.put("films", startIndex < endIndex ? 
            filteredFilms.subList(startIndex, endIndex) : 
            new ArrayList<>());
        
        return response;
    }

    private Long generateUniqueId() {
        // Générer un ID unique, par exemple en utilisant l'UUID
        return System.currentTimeMillis(); // Exemple simple, remplacez par votre logique
    }
}
