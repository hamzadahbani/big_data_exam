import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AvgWatchDurationByDay {

    public static class Mapper2 extends Mapper<LongWritable, Text, Text, IntWritable> {
        private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            if (key.get() == 0 && value.toString().contains("user_id")) return;
            String[] fields = value.toString().split(",");
            if (fields.length >= 5) {
                try {
                    int duration = Integer.parseInt(fields[3]);
                    Date date = sdf.parse(fields[4]);
                    String day = dayFormat.format(date);
                    context.write(new Text(day), new IntWritable(duration));
                } catch (Exception ignored) {}
            }
        }
    }

    public static class Reducer2 extends Reducer<Text, IntWritable, Text, DoubleWritable> {
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0, count = 0;
            for (IntWritable val : values) {
                sum += val.get();
                count++;
            }
            context.write(key, new DoubleWritable((double) sum / count));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "Average Watch Duration By Day");
        job.setJarByClass(AvgWatchDurationByDay.class);
        job.setMapperClass(Mapper2.class);
        job.setReducerClass(Reducer2.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
