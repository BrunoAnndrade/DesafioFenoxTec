package com.example.desafiofenoxtec.Presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.desafiofenoxtec.Data.Repository.DataBaseRepository
import com.example.desafiofenoxtec.Data.Local.NewsEntity
import com.example.desafiofenoxtec.Data.Repository.NewsApiRepository
import com.example.desafiofenoxtec.Domain.Util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsListViewModel @Inject constructor(
    private val repository: NewsApiRepository,
    private val dataBaseRepository: DataBaseRepository
) : ViewModel() {

    private val _newsList = MutableStateFlow<List<NewsEntity>>(emptyList())
    val newsList: StateFlow<List<NewsEntity>> = _newsList.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            loadNewsFromDatabase()
            startPeriodicDataUpdate()

        }
    }

    private suspend fun loadNewsFromDatabase() {
        dataBaseRepository.getAllNews().collect { itemList ->
            _newsList.value = itemList
        }
    }

    private fun startPeriodicDataUpdate() {
        viewModelScope.launch {
            while (true) {
                getNewsList() // Obtém as últimas notícias da API e atualiza o banco de dados
                delay(3600000) // Intervalo de uma hora (em milissegundos) - ajuste conforme necessário
            }
        }
    }

    var state by mutableStateOf(NewsRemoteState())
        private set

    fun getNewsList() {
        viewModelScope.launch {
            state = state.copy(
                isLoading = true,
                error = null
            )

            when (val result = repository.getNewsData()) {
                is Resource.Success -> {

                    // Extrair a lista de NewsItem do NewsResponse
                    val newsItems = result.data?.items

                    // Mapear os NewsItem para NewsEntity
                    val newsEntities = newsItems?.map { newsItem ->
                        NewsEntity(
                            id = newsItem.id,
                            introducao = newsItem.introducao,
                            titulo = newsItem.titulo,
                            data_publicacao = newsItem.data_publicacao,
                            image = newsItem.getImageUrl()

                        )
                    }

                    if (newsEntities != null) {
                        saveNewsToDataBase(newsEntities)
                    }

                    state = state.copy(
                        newsResponse = result.data,
                        isLoading = false,
                        error = null
                    )
                }

                is Resource.Error -> {
                    state = state.copy(
                        newsResponse = null,
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    private suspend fun saveNewsToDataBase(newsList:List<NewsEntity>){
        dataBaseRepository.saveNews(newsList)

    }



}