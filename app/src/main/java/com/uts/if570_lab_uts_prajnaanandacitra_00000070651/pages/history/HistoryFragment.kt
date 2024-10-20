package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.history

import HistoryAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentHistoryBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.Attendance
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.AttendanceItem
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.history.carousel.CarouselAdapter

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HistoryAdapter
    private var attendanceList: MutableList<AttendanceItem> = mutableListOf()

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)

        binding.hRecyclerView.layoutManager = LinearLayoutManager(context)

        binding.backButtonFromHistory.setOnClickListener {
            findNavController().navigate(R.id.action_historyFragment_to_mainFragment)
        }

        adapter = HistoryAdapter(attendanceList, binding.noDataText, binding.hRecyclerView)
        binding.hRecyclerView.adapter = adapter

        db = FirebaseConfig.getFirestore()
        auth = Firebase.auth

        fetchAttendanceData()

        return binding.root
    }

    private fun fetchAttendanceData() {
        db.collection("attendance")
            .whereEqualTo("userId", auth.currentUser?.uid)
            .get()
            .addOnSuccessListener { result ->
                attendanceList.clear()
                for (document in result) {
                    val attendance = document.toObject(Attendance::class.java)

                    if (attendance.attendanceList.isNotEmpty()) {
                        attendanceList.addAll(attendance.attendanceList)
                    }
                }
                updateRecyclerView()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRecyclerView() {
        if (attendanceList.isEmpty()) {
            binding.noDataText.visibility = View.VISIBLE
            binding.hRecyclerView.visibility = View.GONE
        } else {
            binding.noDataText.visibility = View.GONE
            binding.hRecyclerView.visibility = View.VISIBLE
        }
        adapter.updateAttendanceList(attendanceList)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
