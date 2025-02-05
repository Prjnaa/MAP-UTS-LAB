package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentMainBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.extensions.getCurrentUserFromDB
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.Attendance
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils.sessionCheck
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionCheck()

        auth = FirebaseAuth.getInstance()
        db = FirebaseConfig.getFirestore()

        showUser()
        iconState()
        dateInfo()

        with(binding) {
            greetings.text = getGreetings()

            accountPageButton.setOnClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_accountFragment)
            }

            historyPageButton.setOnClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_historyFragment)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        checkIfAllowed()
    }

    override fun onStart() {
        super.onStart()
        checkIfAllowed()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun showUser() {
        auth.getCurrentUserFromDB(db) { user ->
            user?.let {
                val username = it.username
                val imageUrl = it.imageUrl

                val displayName = username.split(" ").take(2).joinToString(" ")
                binding.userNameView.text = displayName.ifEmpty { "Username Not Set." }

                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .into(binding.userImage)
            } ?: run { binding.userNameView.text = getString(R.string.username_not_set_warning) }
        }
    }

    private fun checkIfAllowed() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("attendance").document(uid)

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        userRef.addSnapshotListener { documentSnapshot, error ->
            if (error != null) {
                updateAttendButtonState(false)
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val attendance = documentSnapshot.toObject(Attendance::class.java)

                attendance?.let {
                    val todayAttendance =
                        it.attendanceList.find { item -> item.date == currentDate }
                    val isAllowed =
                        todayAttendance == null ||
                            (todayAttendance.checkInTime.isNotEmpty() &&
                                todayAttendance.checkOutTime.isEmpty())

                    updateAttendButtonState(isAllowed)
                    updateCheckInOutTimeDisplay()
                } ?: run {
                    updateAttendButtonState(true)
                    updateCheckInOutTimeDisplay()
                }
            } else {
                updateAttendButtonState(true)
                updateCheckInOutTimeDisplay()
            }
        }
    }

    private fun updateAttendButtonState(isAllowed: Boolean) {
        val drawable = binding.attendanceBtn.background
        if (isAllowed) {
            DrawableCompat.setTint(
                drawable, ContextCompat.getColor(requireContext(), R.color.primary))
            binding.attendanceBtn.isEnabled = true
            binding.attendanceBtn.setOnClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_attendanceFragment)
            }
        } else {
            binding.attendanceBtn.isEnabled = false
            DrawableCompat.setTint(
                drawable, ContextCompat.getColor(requireContext(), R.color.danger))
        }
        binding.attendanceBtn.background = drawable
    }

    private fun updateCheckInOutTimeDisplay() {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("attendance").document(uid)

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val attendance = document.toObject(Attendance::class.java)
                attendance?.let {
                    val todayAttendance =
                        it.attendanceList.find { item -> item.date == currentDate }
                    todayAttendance?.let { attendanceItem ->
                        when {
                            attendanceItem.checkInTime.isNotEmpty() &&
                                attendanceItem.checkOutTime.isEmpty() -> {
                                binding.checkInOutTimeDisplay.visibility = View.VISIBLE
                                binding.checkInOutTimeDisplay2.visibility = View.VISIBLE
                                binding.checkInOutTimeDisplay.text =
                                    getString(R.string.check_in_time_text)
                                binding.checkInOutTimeDisplay2.text = attendanceItem.checkInTime
                            }
                            attendanceItem.checkInTime.isNotEmpty() &&
                                attendanceItem.checkOutTime.isNotEmpty() -> {
                                binding.checkInOutTimeDisplay.visibility = View.VISIBLE
                                binding.checkInOutTimeDisplay2.visibility = View.VISIBLE
                                binding.checkInOutTimeDisplay.text =
                                    getString(R.string.check_out_time_text)
                                binding.checkInOutTimeDisplay2.text = attendanceItem.checkOutTime
                            }
                            else -> {
                                binding.checkInOutTimeDisplay.text = ""
                                binding.checkInOutTimeDisplay.visibility = View.GONE
                                binding.checkInOutTimeDisplay2.visibility = View.GONE
                            }
                        }
                    }
                        ?: run {
                            binding.checkInOutTimeDisplay.text = ""
                            binding.checkInOutTimeDisplay.visibility = View.GONE
                            binding.checkInOutTimeDisplay2.visibility = View.GONE
                        }
                }
                    ?: run {
                        binding.checkInOutTimeDisplay.text = ""
                        binding.checkInOutTimeDisplay.visibility = View.GONE
                        binding.checkInOutTimeDisplay2.visibility = View.GONE
                    }
            } else {
                binding.checkInOutTimeDisplay.text = ""
                binding.checkInOutTimeDisplay.visibility = View.GONE
                binding.checkInOutTimeDisplay2.visibility = View.GONE
            }
        }
    }

    private fun getGreetings(): String {
        return when (LocalTime.now().hour) {
            in 3..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun iconState() {
        val iconResId =
            if (LocalTime.now().hour in 6..17) {
                R.drawable.baseline_sunny_40
            } else {
                R.drawable.baseline_bedtime_40
            }

        binding.timeIcon.setImageResource(iconResId)
        val colorResId = if (LocalTime.now().hour in 6..17) R.color.sun else R.color.moon
        binding.timeIcon.drawable.setTint(ContextCompat.getColor(requireContext(), colorResId))
    }

    private fun dateInfo() {
        val date = LocalDate.now()
        val dayFormatter = DateTimeFormatter.ofPattern("EEE")
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
        val yearFormatter = DateTimeFormatter.ofPattern("yyyy")

        binding.dayTextView.text = date.format(dayFormatter)
        binding.dateTextView.text = date.format(dateFormatter)
        binding.yearTextView.text = date.format(yearFormatter)
    }
}
