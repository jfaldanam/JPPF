$template{name="sample-readme-html-header" title="Generation of Mandelbrot Fractal Movies"}$
<h3>What does the sample do?</h3>
This sample demonstrates a JPPF grid based on a volunteer computing model.
Its goal is to generate AVI movies made of fractal images (Mandelbrot set) individually computed as in the <a href="../Fractals/Readme.html">Fractals</a> sample.
<p>For this, we will reuse the code and artifacts of the <a href="../Fractals/Readme.html">Fractals</a> sample, since it already implements JPPF tasks for the Mandelbrot computations, along with the code to submit the corresponding JPPF jobs.
The movie file itself is created using the <a href="http://www.randelshofer.ch/monte/">Monte Media Library</a>.
<p>Additionally, we will also generate a specific node distribution, with the following extensions and customizations:
<ul class="samplesList">
  <li>the node will run in <a href="http://www.jppf.org/doc/v5/index.php?title=Deployment_and_run_modes#Offline_nodes">offline mode</a>: it will disconnect from the server to execute the job, then reconnect to send the results back</li>
  <li>the node will run in <a href="http://www.jppf.org/doc/v5/index.php?title=Deployment_and_run_modes#JPPF_Node_in_.E2.80.9CIdle_Host.E2.80.9D_mode">idle mode</a>:
  it will start when the computer is idle (no user activity) and stop when the user wakes it up</li>
  <li>it will embed a modified version of the <a href="http://www.jppf.org/doc/v5/index.php?title=JPPF_node_screensaver#JPPF_built-in_screensaver">JPPF default screensaver</a>, which will display a preview of the image being computed by the node.
  This relies on <a href="http://www.jppf.org/doc/v5/index.php?title=Task_objects#Sending_notifications_from_a_task">task notifications emitted for each point of the image</a>.
  The screen saver will look like <a href="../shared/images/MandelbrotNode.gif" target="_blank">this screenshot</a>.</li>
  <li>Since the node is offline, and to reduce network traffic, all required libraries and classes will be part of the node's local classpath</li>
</ul>

<h3>How do I run it?</h3>
Before running this sample application, you must have a JPPF server running.<br>
For information on how to set up a server, please refer to the <a href="http://www.jppf.org/doc/v5/index.php?title=Introduction">JPPF documentation</a>.
<p>Once you have a server running, you can build the node distribution by typing <b>ant build</b> from a command or shell prompt.
This will produce two files "<b>JPPF-node.zip</b>" and "<b>JPPF-node.tar.gz</b>". Use the file appropriate for the platform where the node is deployed (the .tar.gz includes execute permission for the startNode.sh script).
If you wish to modify the node settings before this, you can edit the configuration files in <b>FractalMovieGenerator/config/node</b>.<br>

<p>once you have a node running, you can launch the sample by typing:<br/>
<code>&lt;run&gt; -i data/mandelbrot.csv -o data/mandelbrot.avi -f 30 -t 30</code><br/>
where &lt;run&gt; is replaced with "run.bat" on Windows platforms or "./run.sh" on Linux/Unix platforms.
<p>The command lines arguments can be obtained by typing <code>&lt;run&gt; -h</code>, which displays:
<pre Class="samples">usage:
Windows: run.bat [option, ...]
Linux: ./run.sh [option, ...]
There are two possible sets of options:
1. &lt;run_cmd&gt; -h|?
  display this screen and exit
2. &lt;run_cmd&gt; -i &lt;input_file&gt; -o &lt;output_file&gt; -f &lt;frame_rate&gt; -t &lt;trans_time&gt;
where:
  input_file: a csv record file produced by the mandelbrot fractal sample
  output_file: path to the generated movie file
    the .avi extension is added if needed
  frame_rate: number of frames per second
  trans_time: the duration (in seconds) of a transition
    between 2 records in the input file
note: the total number of frames in the generated movie is equal to
  (nb_input_records-1) * frame_rate * trans_time</pre>

<p>It is also possible to specify how many jobs can be sent concurrently by the application, by setting the following in config/jppf.properties:
<pre class="prettyprint lang-conf">
# max number of jobs that can be concurrently submitted
# if &gt; 1, it also requires setting a connection pool
jppf.fractals.concurrent.jobs = 4
</pre>
<p>This mechanism limits the memory consumption of the application, as the generated images can be huge and lead to OutOfMemoryError if too many are generated in parallel.
For instance, to have up to 4 images generated in parallel, you would set the following:
<pre class="prettyprint lang-conf">
jppf.fractals.concurrent.jobs = 4
jppf.pool.size = 4
</pre>
<p>Lastly, you can also configure a job <a href="http://www.jppf.org/doc/v5/index.php?title=Job_Service_Level_Agreement#Expiration_of_job_dispatches">dispatch timeout and maximum allowed timeouts</a> in the client configuration.
These 2 properties are used to prevent jobs from getting stuck forever, in case a node never completes the execution of a job. This is done as follows:
<pre class="prettyprint lang-conf">
# maximum allowed time for a job to execute in a node, before it is resubmitted
# or cancelled; defaults to 15000 ms
jppf.fractals.dispatch.timeout = 15000
# maximum number of allowed timeouts before a job is cancelled; defaults to 1
jppf.fractals.dispatch.max.timeouts = 1
</pre>
<h3>Example generated video</h3>
The following was generated as an image size of 640x360 (360p definition), using a frame rate of 30 fps, and based on the input file "<b>FractalMovieGenerator/data/mandelbrot.csv</b>".
Note that uploading it to Youtube required an additonal encoding step using the H.264 codec to generate a .mp4 video file.
<p><iframe width="640" height="360" src="http://www.youtube.com/embed/JDVBQHkEsMM?feature=player_detailpage" frameborder="0" allowfullscreen></iframe>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the source, or have a look at the <a href="javadoc/index.html">API documentation</a>.
<p>In addition, There are 2 privileged places you can go to:
<ul class="samplesList">
  <li><a href="http://www.jppf.org/forums"/>The JPPF Forums</a></li>
  <li><a href="http://www.jppf.org/doc/v5/">The JPPF documentation</a></li>
</ul>
$template{name="sample-readme-html-footer"}$
