import android.util.Log // Import the Log class
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.AttendanceItem
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.history.carousel.CarouselAdapter

class HistoryAdapter(
    private var attendanceList: List<AttendanceItem>,
    private val emptyStateTextView: TextView,
    private val recyclerView: RecyclerView
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.dateTextView2)
        val checkInTimeText: TextView = itemView.findViewById(R.id.checkInTimeDisplay)
        val checkOutTimeText: TextView = itemView.findViewById(R.id.checkOutTimeDisplay)
        val carouselView: ViewPager2 = itemView.findViewById(R.id.imageCarousel)
    }

    init {
        updateEmptyStateVisibility()
    }

    private fun updateEmptyStateVisibility() {
        if (attendanceList.isEmpty()) {
            emptyStateTextView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateTextView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val attendanceItem = attendanceList[position]
        holder.dateText.text = attendanceItem.date
        holder.checkInTimeText.text = attendanceItem.checkInTime
        holder.checkOutTimeText.text = attendanceItem.checkOutTime

        val imagePaths = listOf(attendanceItem.checkInPhotoUrl, attendanceItem.checkOutPhotoUrl)

        if (imagePaths.all { it.isEmpty() }) {
            holder.carouselView.adapter = CarouselAdapter(emptyList())
            return
        }

        fetchImageFromStorage(imagePaths) { imageUrls ->
            val carouselAdapter = CarouselAdapter(imageUrls)
            holder.carouselView.adapter = carouselAdapter
        }
    }

    override fun getItemCount(): Int {
        return attendanceList.size
    }

    fun updateAttendanceList(newAttendanceList: List<AttendanceItem>) {
        attendanceList = newAttendanceList
        updateEmptyStateVisibility()
        notifyDataSetChanged()
    }

    private fun fetchImageFromStorage(paths: List<String>, callback: (List<String>) -> Unit) {
        val storage = FirebaseConfig.getStorage()

        val imageUrls = mutableListOf<String>()
        val storageRef = storage.reference

        var imageFetched = 0

        if (paths.isEmpty()) {
            callback(imageUrls)
            return
        }

        for (path in paths) {
            if (path.isNotEmpty()) {
                val imageRef = storageRef.child("attendance/$path")

                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        imageUrls.add(uri.toString())
                        imageFetched++

                        if (imageFetched == paths.size) {
                            callback(imageUrls)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("HistoryAdapter", "Error fetching image for path: $path", e)
                        imageFetched++

                        if (imageFetched == paths.size) {
                            callback(imageUrls)
                        }
                    }
            } else {
                imageFetched++
                if (imageFetched == paths.size) {
                    callback(imageUrls)
                }
            }
        }
    }
}
