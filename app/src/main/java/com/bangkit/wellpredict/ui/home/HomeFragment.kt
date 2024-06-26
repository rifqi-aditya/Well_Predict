package com.bangkit.wellpredict.ui.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bangkit.wellpredict.data.ResultState
import com.bangkit.wellpredict.data.api.response.HistoryItem
import com.bangkit.wellpredict.databinding.FragmentHomeBinding
import com.bangkit.wellpredict.ui.ViewModelFactory
import com.bangkit.wellpredict.ui.auth.LoginActivity
import com.bangkit.wellpredict.ui.history.HistoryActivity
import com.bangkit.wellpredict.ui.history.HistoryDetailActivity
import com.bangkit.wellpredict.ui.news.NewsActivity
import com.bangkit.wellpredict.ui.news.NewsWebViewActivity
import com.bangkit.wellpredict.utils.DateHelper
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout


class HomeFragment : Fragment() {

    private val viewModel by viewModels<HomeViewModel> {
        ViewModelFactory.getInstance(requireContext())
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var shimmerNewsFrameLayout: ShimmerFrameLayout
    private lateinit var shimmerHistoryFrameLayout: ShimmerFrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        shimmerNewsFrameLayout = binding.cvShimmerNews
        shimmerHistoryFrameLayout = binding.cvShimmerHistory

        checkUserIsLogin()
        setupNewsCard()
        setupHistoryCard()
        setupSeeAllOnClickHandler(binding.tvNewsSeeAll, NewsActivity::class.java)
        setupSeeAllOnClickHandler(binding.tvDiagnoseHistorySeeAll, HistoryActivity::class.java)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkUserIsLogin(){
        viewModel.getSession().observe(viewLifecycleOwner) { user ->
            if (!user.isLogin) {
                startActivity(Intent(requireContext(), LoginActivity::class.java))
            }
            binding.tvUserName.text = user.name
        }
    }

    private fun setupNewsCard() {
        viewModel.getNewsFirst().observe(viewLifecycleOwner) { result ->
            if (result != null) {
                when (result) {
                    is ResultState.Loading -> {
                        showNewsCardLoading(true, binding.newsCardView)
                    }

                    is ResultState.Success -> {
                        showNewsCardLoading(false, binding.newsCardView)

                        binding.tvNewsAuthor.text = result.data.source?.name
                        binding.tvNewsTitle.text = result.data.title
                        binding.tvNewsUploadTime.text =
                            DateHelper.convertTime(result.data.publishedAt.toString())
                        context?.let {
                            Glide.with(it)
                                .load(result.data.urlToImage)
                                .into(binding.ivNewsImage)
                        }
                        newsCardOnClickHandler(result.data.url.toString())
                    }

                    is ResultState.Error -> {
                        showNewsCardLoading(false, binding.newsCardView)
                        showAlert(result.error)
                    }
                }
            }
        }
    }

    private fun setupHistoryCard() {
        viewModel.getHistoryFirst().observe(viewLifecycleOwner) { result ->
            when (result) {
                is ResultState.Loading -> {
                    // Sembunyikan history card dan empty title saat loading
                    binding.diagnoseHistoryCard.visibility = View.INVISIBLE
                    binding.tvDiagnoseHistoryEmptyTitle.visibility = View.GONE
                    showHistoryCardLoading(true)
                }

                is ResultState.Success -> {
                    // Tampilkan history card dan sembunyikan empty title serta shimmer saat sukses
                    binding.tvDiagnoseHistoryEmptyTitle.visibility = View.GONE
                    showHistoryCardLoading(false)

                    binding.diagnoseHistoryCard.visibility = View.VISIBLE
                    binding.tvDiseaseTitle.text = result.data.disease
                    binding.tvDiseaseDate.text = result.data.createdAt?.let { DateHelper.formatDateToLocal(it) }
                    diagnoseCardOnClickHandler(result.data)
                }

                is ResultState.Error -> {
                    // Sembunyikan history card, tampilkan empty title dan stop shimmer saat error
                    binding.diagnoseHistoryCard.visibility = View.GONE
                    binding.tvDiagnoseHistoryEmptyTitle.visibility = View.VISIBLE
                    showHistoryCardLoading(false)
                }
            }
        }
    }


    private fun setupSeeAllOnClickHandler(view: View, targetActivity: Class<out Activity>) {
        view.setOnClickListener {
            val intent = Intent(activity, targetActivity)
            startActivity(intent)
        }
    }

    private fun newsCardOnClickHandler(newsUrl: String) {
        val newsCard = binding.newsCardView
        newsCard.setOnClickListener {
            val intent = Intent(activity, NewsWebViewActivity::class.java)
            intent.putExtra("NEWS_URL", newsUrl)
            startActivity(intent)
        }
    }

    private fun diagnoseCardOnClickHandler(historyItem: HistoryItem) {
        val historyCard = binding.diagnoseHistoryCard
        historyCard.setOnClickListener {
            val intent = Intent(activity, HistoryDetailActivity::class.java)
            val historyItems = ArrayList<HistoryItem>()
            historyItems.add(historyItem)
            intent.putParcelableArrayListExtra("HISTORY_ITEMS", historyItems)
            startActivity(intent)
        }
    }

    private fun showNewsCardLoading(isLoading: Boolean, targetLayout: View) {
        if (isLoading) {
            targetLayout.visibility = View.INVISIBLE
            shimmerNewsFrameLayout.visibility = View.VISIBLE
            shimmerNewsFrameLayout.startShimmer()
        } else {
            shimmerNewsFrameLayout.stopShimmer()
            shimmerNewsFrameLayout.visibility = View.GONE
            targetLayout.visibility = View.VISIBLE
        }
    }

    private fun showHistoryCardLoading(isLoading: Boolean) {
        if (isLoading) {
            shimmerHistoryFrameLayout.visibility = View.VISIBLE
            shimmerHistoryFrameLayout.startShimmer()
        } else {
            shimmerHistoryFrameLayout.stopShimmer()
            shimmerHistoryFrameLayout.visibility = View.GONE
        }
    }

    private fun showAlert(message: String) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle(message)
            setPositiveButton("OK") { _, _ -> }
            create()
            show()
        }
    }
}